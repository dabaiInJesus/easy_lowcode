package com.dabai.easy_lowcode.dashboard.controller;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlRequest;
import com.dabai.easy_lowcode.dashboard.dto.TextToSqlResponse;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngine;
import com.dabai.easy_lowcode.dashboard.engine.SqlEngineFactory;
import com.dabai.easy_lowcode.dashboard.service.SqlExplainService;
import com.dabai.easy_lowcode.dashboard.service.TextToSqlService;
import com.dabai.easy_lowcode.dashboard.service.ai.ChartRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏增强 API
 * 提供 Text-to-SQL、AI 图表推荐、SQL 解释等能力。
 */
@Tag(name = "数据视图增强", description = "Text-to-SQL、SQL解释、图表推荐、数据源连接测试")
@Slf4j
@RestController
@RequestMapping("/api/dataview")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DataViewController {

    private final TextToSqlService textToSqlService;
    private final SqlExplainService sqlExplainService;
    private final ChartRecommendService chartRecommendService;
    private final SqlEngineFactory sqlEngineFactory;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    @Operation(summary = "自然语言转SQL并执行", description = "使用AI将自然语言问题转换为SQL并执行查询")
    @ApiResponse(responseCode = "200", description = "执行成功")
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
            return Result.error("Text-to-SQL 失败: " + e.getMessage());
        }
    }

    @Operation(summary = "AI解释SQL", description = "使用AI解释SQL语句并给出优化建议")
    @ApiResponse(responseCode = "200", description = "解释成功")
    @PostMapping("/sql/explain")
    public Result<SqlExplainService.ExplainResponse> explainSql(@RequestBody SqlExplainService.ExplainRequest request) {
        try {
            SqlExplainService.ExplainResponse response = sqlExplainService.explain(request);
            if (response.error() != null) {
                return Result.error(response.error());
            }
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("SQL 解释失败: " + e.getMessage());
        }
    }

    @Operation(summary = "校验SQL语法", description = "检查SQL语句的语法是否正确")
    @ApiResponse(responseCode = "200", description = "校验完成")
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

    @Operation(summary = "推荐图表类型", description = "根据数据特征AI推荐合适的图表类型")
    @ApiResponse(responseCode = "200", description = "推荐成功")
    @PostMapping("/chart/recommend")
    public Result<ChartRecommendService.ChartRecommendation> recommendChart(
            @RequestBody ChartRecommendService.ChartRecommendRequest request) {
        try {
            var recommendation = chartRecommendService.recommend(request);
            return Result.success(recommendation);
        } catch (Exception e) {
            return Result.error("图表推荐失败: " + e.getMessage());
        }
    }

    @Operation(summary = "测试数据源连接", description = "测试数据源连接是否可用")
    @ApiResponse(responseCode = "200", description = "测试完成")
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
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取数据源列表", description = "获取所有已启用的数据源列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/datasources")
    public Result<List<DataSourceConfig>> listDataSources() {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataSourceConfig>()
                .eq(DataSourceConfig::getStatus, 1)
                .orderByDesc(DataSourceConfig::getCreateTime);
        List<DataSourceConfig> list = dataSourceConfigMapper.selectList(wrapper);
        for (DataSourceConfig ds : list) {
            ds.setPassword("******");
        }
        return Result.success(list);
    }

    @Operation(summary = "获取表字段元数据", description = "获取指定表的列元数据信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/tables/{datasourceId}/columns")
    public Result<List<SqlEngine.ColumnMeta>> getTableColumns(
            @Parameter(description = "数据源ID") @PathVariable Long datasourceId,
            @Parameter(description = "表名", required = true) @RequestParam String table,
            @Parameter(description = "Schema名称") @RequestParam(required = false) String schema) {
        try {
            DataSourceConfig config = dataSourceConfigMapper.selectById(datasourceId);
            if (config == null) {
                return Result.error("数据源不存在");
            }
            SqlEngine engine = sqlEngineFactory.getEngine(config);
            List<SqlEngine.ColumnMeta> columns = engine.getColumns(schema, table);
            return Result.success(columns);
        } catch (Exception e) {
            return Result.error("获取字段失败: " + e.getMessage());
        }
    }
}
