package com.dabai.easy_lowcode.dashboard.service;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlRequest;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlResponse;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngine;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngineFactory;
import com.dabai.easy_lowcode.dashboard.service.ai.ChartRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text-to-SQL 服务
 * <p>
 * 将自然语言转换为 SQL 并执行。
 * 工作流程：
 * <ol>
 *   <li>获取表的字段元数据</li>
 *   <li>构建提示词（含字段信息）</li>
 *   <li>调用 AI 生成 SQL</li>
 *   <li>验证并执行 SQL</li>
 *   <li>返回结果和建议的图表类型</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextToSqlService {

    private final ChatModel defaultChatModel;  // 默认 AI 模型（通义千问）
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final SqlEngineFactory sqlEngineFactory;
    private final ChartRecommendService chartRecommendService;

    /**
     * 自然语言转 SQL 并可选执行
     */
    public TextToSqlResponse textToSql(TextToSqlRequest request) {
        TextToSqlResponse response = new TextToSqlResponse();

        // 1. 获取数据源配置
        DataSourceConfig ds = dataSourceConfigMapper.selectById(request.getDatasourceId());
        if (ds == null) {
            throw new BusinessException("数据源不存在: id=" + request.getDatasourceId());
        }

        SqlEngine engine = sqlEngineFactory.getEngine(ds);

        // 2. 获取表字段元数据，构建上下文
        List<SqlEngine.ColumnMeta> columns = engine.getColumns(null, request.getTableName());
        if (columns.isEmpty()) {
            // 尝试从 SQL 方言推断
            log.warn("无法获取表 {} 的字段信息，将使用通用提示", request.getTableName());
        }

        // 3. 构建提示词
        String prompt = buildPrompt(request.getQuestion(), request.getTableName(), columns, engine.getDialect());

        // 4. 调用 AI 生成 SQL
        String aiReply;
        try {
            List<Message> messages = List.of(
                    new SystemMessage("你是一个专业的SQL工程师，擅长根据用户需求生成高效准确的SQL查询语句。"),
                    new UserMessage(prompt)
            );

            var aiResponse = defaultChatModel.call(new org.springframework.ai.chat.prompt.Prompt(messages));
            aiReply = aiResponse.getResult().getOutput().getText();
            log.info("AI 回复原始内容: {}", aiReply);
        } catch (Exception e) {
            log.error("AI 生成 SQL 失败", e);
            response.setSuccess(false);
            response.setErrorMessage("AI 生成 SQL 失败: " + e.getMessage());
            return response;
        }

        // 5. 提取 SQL
        String sql = extractSql(aiReply);
        if (sql == null || sql.isBlank()) {
            response.setSuccess(false);
            response.setErrorMessage("AI 未生成有效 SQL，原始回复: " + aiReply);
            response.setAiContent(aiReply);
            return response;
        }
        response.setSql(sql);
        response.setAiContent(aiReply);

        // 6. 验证并执行 SQL
        if (request.isExecute()) {
            try {
                List<Map<String, Object>> data = engine.execute(sql, request.getLimit());
                response.setData(data);
                response.setRowCount(data.size());
                response.setSuccess(true);

                // 7. 推荐图表类型
                if (!data.isEmpty()) {
                    var chartType = chartRecommendService.recommend(data, request.getLimit());
                    response.setRecommendedChartType(chartType.chartType());
                    response.setRecommendedEchartsOption(chartType.echartsOption());
                }
            } catch (Exception e) {
                log.error("SQL 执行失败: {}", sql, e);
                response.setSuccess(false);
                response.setErrorMessage("SQL 执行失败: " + e.getMessage() + "\n生成的SQL: " + sql);
            }
        } else {
            response.setSuccess(true);
        }

        return response;
    }

    /**
     * 构建 AI 提示词
     */
    private String buildPrompt(String question, String tableName,
                                List<SqlEngine.ColumnMeta> columns, SqlEngine.SqlDialect dialect) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("问题：").append(question).append("\n");
        prompt.append("目标表：").append(tableName).append("\n");
        prompt.append("数据库类型：").append(dialect.getDisplayName()).append("\n\n");

        if (!columns.isEmpty()) {
            prompt.append("表结构（字段名 / 类型 / 注释）：\n");
            for (SqlEngine.ColumnMeta col : columns) {
                prompt.append(String.format("  - %s (%s)%s%n",
                        col.name(), col.type(),
                        col.comment() != null && !col.comment().isBlank() ? " // " + col.comment() : ""));
            }
            prompt.append("\n");
        } else {
            prompt.append("表结构未知，请根据常识生成合理的 SQL。\n\n");
        }

        prompt.append("要求：\n");
        prompt.append("1. 只返回 SQL 语句，不要其他解释\n");
        prompt.append("2. 确保 SQL 语法正确，适用于 ").append(dialect.getDisplayName()).append("\n");
        prompt.append("3. 只做查询，不要增删改\n");
        prompt.append("4. 返回格式：用 ```sql ... ``` 包裹 SQL\n\n");
        prompt.append("请生成 SQL：");

        return prompt.toString();
    }

    /**
     * 从 AI 回复中提取 SQL 语句
     * 支持格式：
     * - ```sql ... ```
     * - ``` ... ```
     * - 纯 SQL（没有包裹）
     */
    private String extractSql(String aiReply) {
        if (aiReply == null) return null;

        // 尝试提取 ```sql ... ```
        Pattern p1 = Pattern.compile("```sql\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(aiReply);
        if (m1.find()) {
            return cleanSql(m1.group(1).trim());
        }

        // 尝试提取 ``` ... ```
        Pattern p2 = Pattern.compile("```\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(aiReply);
        if (m2.find()) {
            return cleanSql(m2.group(1).trim());
        }

        // 兜底：去掉 AI 回复的前后非 SQL 部分
        String cleaned = aiReply.trim();
        // 去掉开头的解释性文字
        int firstSelect = cleaned.toLowerCase().indexOf("select");
        if (firstSelect >= 0) {
            cleaned = cleaned.substring(firstSelect);
        }
        return cleanSql(cleaned);
    }

    /**
     * 清理提取的 SQL（去除多余空白、换行等）
     */
    private String cleanSql(String sql) {
        if (sql == null) return null;
        // 去除首尾空白
        sql = sql.trim();
        // 去除结尾的分号多余
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }
}
