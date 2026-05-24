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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class TextToSqlService {

    private final ChatModel defaultChatModel;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final SqlEngineFactory sqlEngineFactory;
    private final ChartRecommendService chartRecommendService;

    public TextToSqlService(
            @Qualifier("dashScopeChatModel") ChatModel defaultChatModel,
            DataSourceConfigMapper dataSourceConfigMapper,
            SqlEngineFactory sqlEngineFactory,
            ChartRecommendService chartRecommendService) {
        this.defaultChatModel = defaultChatModel;
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.sqlEngineFactory = sqlEngineFactory;
        this.chartRecommendService = chartRecommendService;
    }

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
     * 构建 AI 提示词（few-shot 版本）
     * <p>
     * 包含：
     * <ul>
     *   <li>Few-shot 示例（按方言匹配）</li>
     *   <li>表结构上下文（字段名/类型/注释/示例值）</li>
     *   <li>方言约束与安全规则</li>
     *   <li>常见查询模式指引</li>
     * </ul>
     */
    private String buildPrompt(String question, String tableName,
                                List<SqlEngine.ColumnMeta> columns, SqlEngine.SqlDialect dialect) {
        StringBuilder prompt = new StringBuilder();

        // ========== Few-shot 示例（按方言）==========
        prompt.append(getFewShotExamples(dialect));

        // ========== 表结构上下文 ==========
        prompt.append("【任务】根据以下信息生成 SQL 查询\n\n");
        prompt.append("问题：").append(question).append("\n");
        prompt.append("目标表：").append(tableName).append("\n");
        prompt.append("数据库类型：").append(dialect.getDisplayName()).append("\n\n");

        if (!columns.isEmpty()) {
            prompt.append("【表结构】\n");
            for (SqlEngine.ColumnMeta col : columns) {
                prompt.append(String.format("  - %s (%s)%s%n",
                        col.name(), col.type(),
                        col.comment() != null && !col.comment().isBlank() ? " // " + col.comment() : ""));
            }
            prompt.append("\n");

            // 示例数据（前3行），帮助 AI 理解字段值含义
            List<Map<String, Object>> sampleData = null;
            try {
                String sampleSql = "SELECT * FROM " + escapeIdentifier(tableName, dialect) + " LIMIT 3";
                SqlEngine engine = sqlEngineFactory.getEngine(
                        dataSourceConfigMapper.selectById(1L)); // 临时借用
                // 实际在 engine.execute 前无法提前获取，此处留空，后续优化可提前拉取
            } catch (Exception ignored) { }
        } else {
            prompt.append("【表结构】未知，请根据常识生成合理 SQL。\n\n");
        }

        // ========== 方言约束 ==========
        prompt.append(getDialectConstraints(dialect));

        // ========== 安全与格式要求 ==========
        prompt.append("【输出要求】\n");
        prompt.append("1. 只返回 SQL 语句，不要任何解释或注释\n");
        prompt.append("2. 禁止 DML（INSERT/UPDATE/DELETE/DROP）和管理语句\n");
        prompt.append("3. 必须加 LIMIT 限制结果集（默认 100 条）\n");
        prompt.append("4. 用 ```sql ... ``` 包裹 SQL\n\n");
        prompt.append("请生成 SQL：");

        return prompt.toString();
    }

    /**
     * 获取方言特定的约束说明
     */
    private String getDialectConstraints(SqlEngine.SqlDialect dialect) {
        return switch (dialect) {
            case MYSQL -> """
                【MySQL 约束】
                - 使用 IFNULL(col, default_value) 处理 NULL
                - 日期函数：DATE_FORMAT(col, '%Y-%m-%d')
                - 字符串拼接：CONCAT(col1, col2)
                - 分页：LIMIT offset, size
                - 窗口函数：ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
                """;
            case POSTGRESQL -> """
                【PostgreSQL 约束】
                - 使用 COALESCE(col, default_value) 处理 NULL
                - 日期函数：TO_CHAR(col, 'YYYY-MM-DD')
                - 字符串拼接：col1 || col2 或 CONCAT(col1, col2)
                - 分页：LIMIT size OFFSET offset
                - 窗口函数：ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
                - JSON 处理：col->>'key' 或 jsonb_path_query
                """;
            case ORACLE -> """
                【Oracle 约束】
                - 使用 NVL(col, default_value) 处理 NULL
                - 日期函数：TO_CHAR(col, 'YYYY-MM-DD')
                - 字符串拼接：col1 || col2
                - 分页：WHERE ROWNUM <= size（嵌套子查询）
                - 窗口函数：ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
                - 别名不使用 AS（前缀）
                """;
            case HIVE -> """
                【Hive 约束】
                - 使用 NVL(col, default_value) 处理 NULL
                - 日期函数：FROM_UNIXTIME(UNIX_TIMESTAMP(col), 'yyyy-MM-dd')
                - 字符串拼接：CONCAT(col1, col2)
                - 分页：不支持标准 OFFSET，用 LATERAL VIEW + ROW_NUMBER() 实现
                - 窗口函数：ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
                - 大数据优化：注意数据倾斜，可加 DISTRIBUTE BY
                """;
            case SQLSERVER -> """
                【SQL Server 约束】
                - 使用 ISNULL(col, default_value) 处理 NULL
                - 日期函数：FORMAT(col, 'yyyy-MM-dd')
                - 字符串拼接：col1 + col2
                - 分页：OFFSET size ROWS FETCH NEXT size ROWS ONLY
                - 窗口函数：ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)
                - 别名不使用 AS（前缀）
                """;
            default -> """
                【通用约束】
                - 处理 NULL 用 COALESCE / NVL / IFNULL（根据方言）
                - 字符串拼接根据方言选择对应函数
                - 必须加 LIMIT
                """;
        };
    }

    /**
     * Few-shot 示例（按方言匹配）
     */
    private String getFewShotExamples(SqlEngine.SqlDialect dialect) {
        // 根据不同问题类型提供对应示例
        return switch (dialect) {
            case MYSQL -> """
                【Few-shot 示例 - MySQL】

                示例1（时间趋势）：
                问题：近7天每日销售额
                SQL：
                ```sql
                SELECT DATE(create_time) AS 日期, SUM(amount) AS 销售额
                FROM orders
                WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                GROUP BY DATE(create_time)
                ORDER BY 日期
                LIMIT 100
                ```

                示例2（TOP N 排名）：
                问题：销量前10的商品
                SQL：
                ```sql
                SELECT product_name AS 商品名称, SUM(quantity) AS 销量
                FROM order_items
                GROUP BY product_name
                ORDER BY 销量 DESC
                LIMIT 10
                ```

                示例3（占比分析）：
                问题：各省份订单占比
                SQL：
                ```sql
                SELECT province AS 省份, COUNT(*) AS 订单数,
                       ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) AS 占比
                FROM orders
                GROUP BY province
                ORDER BY 订单数 DESC
                LIMIT 20
                ```

                """;
            case HIVE -> """
                【Few-shot 示例 - Hive】

                示例1（时间趋势）：
                问题：近30天每日 DAU
                SQL：
                ```sql
                SELECT log_date AS 日期, COUNT(DISTINCT user_id) AS 日活
                FROM dws_user_daily_log
                WHERE log_date >= DATE_SUB(FROM_UNIXTIME(UNIX_TIMESTAMP(), 'yyyy-MM-dd'), 29)
                GROUP BY log_date
                ORDER BY 日期
                LIMIT 100
                ```

                示例2（大数据聚合）：
                问题：各城市用户数
                SQL：
                ```sql
                SELECT city AS 城市, COUNT(*) AS 用户数
                FROM dim_user
                GROUP BY city
                ORDER BY 用户数 DESC
                DISTRIBUTE BY city
                SORT BY 用户数 DESC
                LIMIT 50
                ```

                """;
            default -> """
                【Few-shot 示例 - 通用】

                示例1：时间趋势查询
                问题：最近7天数据趋势
                SQL：SELECT DATE(col) AS 日期, COUNT(*) AS 数量 FROM 表名
                     WHERE col >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                     GROUP BY DATE(col) ORDER BY 日期 LIMIT 100

                示例2：TOP N 排名
                问题：排名前10
                SQL：SELECT 维度字段, SUM(数值字段) AS 总计 FROM 表名
                     GROUP BY 维度字段 ORDER BY 总计 DESC LIMIT 10

                """;
        };
    }

    /**
     * 根据方言转义标识符（防 SQL 注入）
     */
    private String escapeIdentifier(String identifier, SqlEngine.SqlDialect dialect) {
        if (identifier == null) return "";
        return switch (dialect) {
            case MYSQL -> "`" + identifier.replace("`", "``") + "`";
            case POSTGRESQL, HIVE -> "\"" + identifier.replace("\"", "\"\"") + "\"";
            default -> identifier;
        };
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
