package com.dabai.easy_lowcode.dashboard.service;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngine;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SQL 解释与优化服务
 * <p>
 * 提供以下能力：
 * <ul>
 *   <li>AI 解释 SQL 语义</li>
 *   <li>SQL 语法检查</li>
 *   <li>性能优化建议</li>
 *   <li>SQL 方言改写建议</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExplainService {

    private final ChatModel defaultChatModel;
    private final SqlEngineFactory sqlEngineFactory;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    /**
     * 解释/优化请求
     *
     * @param sql           SQL 语句
     * @param dialect       目标方言（mysql/postgresql/hive/oracle 等）
     * @param datasourceId  可选，有值时执行 SQL 返回样本数据
     */
    public record ExplainRequest(
            String sql,
            String dialect,
            Long datasourceId
    ) {}

    /**
     * 解释/优化响应
     */
    public record ExplainResponse(
            String explanation,
            List<String> suggestions,
            String rewrittenSql,
            boolean syntaxValid,
            List<Map<String, Object>> sampleData,
            String error
    ) {}

    /**
     * 解释 SQL
     */
    public ExplainResponse explain(ExplainRequest request) {
        if (request.sql() == null || request.sql().isBlank()) {
            return new ExplainResponse(null, List.of(), null, false, null, "SQL 不能为空");
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请分析以下 SQL 语句：\n\n");
        promptBuilder.append("```sql\n").append(request.sql()).append("\n```\n\n");

        if (request.dialect() != null && !request.dialect().isBlank()) {
            promptBuilder.append("目标方言：").append(request.dialect()).append("\n\n");
        }

        promptBuilder.append("请提供：\n");
        promptBuilder.append("1. SQL 语义解释（这段 SQL 做了什么）\n");
        promptBuilder.append("2. 语法是否正确\n");
        promptBuilder.append("3. 性能优化建议（如果有）\n");
        promptBuilder.append("4. 如果方言不匹配，给出目标方言的改写版本（用 ```sql 包裹）\n\n");
        promptBuilder.append("请用中文回答。");

        try {
            List<Message> messages = List.of(
                    new SystemMessage("你是一个专业的数据库工程师，擅长 SQL 分析与优化。"),
                    new UserMessage(promptBuilder.toString())
            );

            var aiResponse = defaultChatModel.call(
                    new org.springframework.ai.chat.prompt.Prompt(messages));
            String aiReply = aiResponse.getResult().getOutput().getText();

            String rewrittenSql = extractSql(aiReply);
            List<String> suggestions = extractSuggestions(aiReply);

            // 有 datasourceId 时，尝试执行样本查询
            List<Map<String, Object>> sampleData = null;
            if (request.datasourceId() != null) {
                sampleData = executeSample(request.datasourceId(), request.sql());
            }

            return new ExplainResponse(aiReply, suggestions, rewrittenSql, true, sampleData, null);

        } catch (Exception e) {
            log.error("SQL 解释失败", e);
            return new ExplainResponse(null, List.of(), null, false, null,
                    "AI 解释失败: " + e.getMessage());
        }
    }

    /**
     * 校验 SQL 语法
     */
    public boolean validateSyntax(String sql, String dialect) {
        try {
            String prompt = String.format("""
                    请判断以下 SQL 语法是否正确（目标方言：%s）。
                    只回答"正确"或"错误 + 原因"，不要其他内容。

                    ```sql
                    %s
                    ```
                    """, dialect != null ? dialect : "通用SQL", sql);

            List<Message> messages = List.of(
                    new SystemMessage("你是 SQL 语法检查器。"),
                    new UserMessage(prompt)
            );

            var response = defaultChatModel.call(
                    new org.springframework.ai.chat.prompt.Prompt(messages));
            String reply = response.getResult().getOutput().getText();
            return reply != null && reply.contains("正确") && !reply.contains("错误");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行样本查询（有异常不抛，只返回空）
     */
    private List<Map<String, Object>> executeSample(Long datasourceId, String sql) {
        try {
            Supplier<DataSourceConfig> configSupplier = () -> dataSourceConfigMapper.selectById(datasourceId);
            SqlEngine engine = sqlEngineFactory.getEngine(datasourceId, configSupplier);
            return engine.execute(sql, 5);
        } catch (Exception e) {
            log.warn("执行样本查询失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractSql(String aiReply) {
        if (aiReply == null) return null;
        var p = java.util.regex.Pattern.compile(
                "```sql\\s*([\\s\\S]*?)```", java.util.regex.Pattern.CASE_INSENSITIVE);
        var m = p.matcher(aiReply);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private List<String> extractSuggestions(String aiReply) {
        List<String> suggestions = new java.util.ArrayList<>();
        if (aiReply == null) return suggestions;
        for (String line : aiReply.split("\n")) {
            line = line.trim();
            if (line.contains("建议") || line.contains("优化") || line.contains("性能")) {
                suggestions.add(line);
            }
        }
        return suggestions;
    }
}
