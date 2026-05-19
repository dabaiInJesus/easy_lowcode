package com.dabai.easy_lowcode.dashboard.controller;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlRequest;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlResponse;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngine;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngineFactory;
import com.dabai.easy_lowcode.dashboard.service.SqlExplainService;
import com.dabai.easy_lowcode.dashboard.service.TextToSqlService;
import com.dabai.easy_lowcode.dashboard.service.ai.ChartRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏增强 API
 * <p>
 * 提供 Text-to-SQL、AI 图表推荐、SQL 解释等能力。
 */
@Slf4j
@RestController
@RequestMapping("/api/dataview")
@RequiredArgsConstructor
public class DataViewController {

    private final TextToSqlService textToSqlService;
    private final SqlExplainService sqlExplainService;
    private final ChartRecommendService chartRecommendService;
    private final SqlEngineFactory sqlEngineFactory;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    // ========== Text-to-SQL ==========

    /**
     * 自然语言转 SQL 并执行
     *
     * POST /api/dataview/text-to-sql
     * Body: { datasourceId, tableName, question, limit, execute }
     */
    @PostMapping("/text-to-sql")
    public Result<TextToSqlResponse> textToSql(@RequestBody TextToSqlRequest request) {
        try {
            TextToSqlResponse response = textToSqlService.textToSql(request);
            if (response.isSuccess()) {
                return Result.success(response);
            } else {
                return Result.error(response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Text-to-SQL 失败", e);
            return Result.error("Text-to-SQL 失败: " + e.getMessage());
        }
    }

    // ========== SQL 解释与纠错 ==========

    /**
     * AI 解释 SQL 并给出优化建议
     *
     * POST /api/dataview/sql/explain
     * Body: { sql, dialect, datasourceId }
     */
    @PostMapping("/sql/explain")
    public Result<SqlExplainService.ExplainResponse> explainSql(@RequestBody SqlExplainService.ExplainRequest request) {
        try {
            SqlExplainService.ExplainResponse response = sqlExplainService.explain(request);
            if (response.error() != null) {
                return Result.error(response.error());
            }
            return Result.success(response);
        } catch (Exception e) {
            log.error("SQL 解释失败", e);
            return Result.error("SQL 解释失败: " + e.getMessage());
        }
    }

    /**
     * 校验 SQL 语法
     *
     * POST /api/dataview/sql/validate
     * Body: { sql, dialect }
     */
    @PostMapping("/sql/validate")
    public Result<Boolean> validateSql(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        String dialect = body.get("dialect");
        if (sql == null || sql.isBlank()) {
            return Result.error("SQL 不能为空");
        }
        boolean valid = sqlExplainService.validateSyntax(sql, dialect);
        return Result.success(valid);
    }

    // ========== AI 图表推荐 ==========

    /**
     * 根据数据特征推荐图表类型
     *
     * POST /api/dataview/chart/recommend
     * Body: { fields: [{name, type, distinctCount, sampleValue}], rowCount }
     */
    @PostMapping("/chart/recommend")
    public Result<ChartRecommendService.ChartRecommendation> recommendChart(
            @RequestBody ChartRecommendService.ChartRecommendRequest request) {
        try {
            var recommendation = chartRecommendService.recommend(request);
            return Result.success(recommendation);
        } catch (Exception e) {
            log.error("图表推荐失败", e);
            return Result.error("图表推荐失败: " + e.getMessage());
        }
    }

    // ========== 数据源连接测试 ==========

    /**
     * 测试数据源连接
     *
     * POST /api/dataview/datasource/test
     * Body: { datasourceId } 或 { dbType, url, username, password }
     */
    @PostMapping("/datasource/test")
    public Result<Boolean> testDataSource(@RequestBody Map<String, Object> body) {
        try {
            DataSourceConfig config;
            if (body.containsKey("datasourceId")) {
                Long id = Long.valueOf(body.get("datasourceId").toString());
                config = dataSourceConfigMapper.selectById(id);
            } else {
                config = new DataSourceConfig();
                config.setDbType((String) body.get("dbType"));
                config.setUrl((String) body.get("url"));
                config.setUsername((String) body.get("username"));
                config.setPassword((String) body.get("password"));
            }

            if (config == null) {
                return Result.error("数据源配置不能为空");
            }

            SqlEngine engine = sqlEngineFactory.getEngine(config);
            boolean ok = engine.testConnection();
            return ok ? Result.success("连接成功", true) : Result.error("连接失败");
        } catch (Exception e) {
            log.error("数据源测试失败", e);
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源列表
     *
     * GET /api/dataview/datasources
     */
    @GetMapping("/datasources")
    public Result<List<DataSourceConfig>> listDataSources() {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataSourceConfig>()
                .eq(DataSourceConfig::getStatus, 1)
                .orderByDesc(DataSourceConfig::getCreateTime);
        List<DataSourceConfig> list = dataSourceConfigMapper.selectList(wrapper);
        // 隐藏密码
        for (DataSourceConfig ds : list) {
            ds.setPassword("******");
        }
        return Result.success(list);
    }

    /**
     * 获取表字段元数据
     *
     * GET /api/dataview/tables/{datasourceId}/columns?table=xxx&schema=default
     */
    @GetMapping("/tables/{datasourceId}/columns")
    public Result<List<SqlEngine.ColumnMeta>> getTableColumns(
            @PathVariable Long datasourceId,
            @RequestParam String table,
            @RequestParam(required = false) String schema) {
        try {
            DataSourceConfig config = dataSourceConfigMapper.selectById(datasourceId);
            if (config == null) {
                return Result.error("数据源不存在");
            }
            SqlEngine engine = sqlEngineFactory.getEngine(config);
            List<SqlEngine.ColumnMeta> columns = engine.getColumns(schema, table);
            return Result.success(columns);
        } catch (Exception e) {
            log.error("获取表字段失败: datasourceId={}, table={}", datasourceId, table, e);
            return Result.error("获取字段失败: " + e.getMessage());
        }
    }
}
