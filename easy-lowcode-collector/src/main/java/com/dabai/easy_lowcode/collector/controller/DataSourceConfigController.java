package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.service.DataSourceConfigService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置控制器
 */
@Tag(name = "数据源管理", description = "数据源CRUD、连接测试、表扫描")
@Slf4j
@RestController
@RequestMapping("/api/collector/datasource")
@RequiredArgsConstructor
public class DataSourceConfigController {
    
    private final DataSourceConfigService dataSourceConfigService;
    private final JdbcTemplate jdbcTemplate;
    
    @Operation(summary = "分页查询数据源列表", description = "分页查询数据源，支持关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<DataSourceConfig>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（名称或编码）") @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DataSourceConfig::getName, keyword)
                   .or()
                   .like(DataSourceConfig::getCode, keyword);
        }
        wrapper.orderByDesc(DataSourceConfig::getCreateTime);
        
        Page<DataSourceConfig> page = dataSourceConfigService.page(new Page<>(current, size), wrapper);
        
        page.getRecords().forEach(ds -> ds.setPassword("******"));
        
        PageResult<DataSourceConfig> result = new PageResult<>(
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getRecords()
        );
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取数据源详情", description = "根据ID获取数据源详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<DataSourceConfig> getById(@Parameter(description = "数据源ID") @PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigService.getById(id);
        return Result.success(config);
    }
    
    @Operation(summary = "创建数据源", description = "创建新的数据源配置")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> create(@RequestBody DataSourceConfig config) {
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            return Result.error("数据源名称不能为空");
        }
        if (config.getCode() == null || config.getCode().trim().isEmpty()) {
            return Result.error("数据源编码不能为空");
        }
        if (config.getDbType() == null || config.getDbType().trim().isEmpty()) {
            return Result.error("数据库类型不能为空");
        }
        if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            return Result.error("连接URL不能为空");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceConfig::getCode, config.getCode());
        if (dataSourceConfigService.count(wrapper) > 0) {
            return Result.error("数据源编码已存在: " + config.getCode());
        }
        
        try {
            config.setPassword(EncryptUtil.encrypt(config.getPassword()));
        } catch (Exception e) {
            return Result.error("密码加密失败");
        }
        
        if (config.getDriverClassName() == null || config.getDriverClassName().isEmpty()) {
            String defaultDriver = getDefaultDriver(config.getDbType());
            if (defaultDriver.isEmpty()) {
                return Result.error("不支持的数据库类型: " + config.getDbType());
            }
            config.setDriverClassName(defaultDriver);
        }
        
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        
        try {
            dataSourceConfigService.save(config);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "更新数据源", description = "更新数据源配置信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> update(@RequestBody DataSourceConfig config) {
        if (config.getId() == null) {
            return Result.error("数据源ID不能为空");
        }
        
        DataSourceConfig existing = dataSourceConfigService.getById(config.getId());
        if (existing == null) {
            return Result.error("数据源不存在");
        }
        
        if (config.getCode() != null && !config.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataSourceConfig::getCode, config.getCode());
            wrapper.ne(DataSourceConfig::getId, config.getId());
            if (dataSourceConfigService.count(wrapper) > 0) {
                return Result.error("数据源编码已存在: " + config.getCode());
            }
        }
        
        LambdaUpdateWrapper<DataSourceConfig> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DataSourceConfig::getId, config.getId());
        
        updateWrapper.set(DataSourceConfig::getName, config.getName())
                    .set(DataSourceConfig::getCode, config.getCode())
                    .set(DataSourceConfig::getDbType, config.getDbType())
                    .set(DataSourceConfig::getUrl, config.getUrl())
                    .set(DataSourceConfig::getUsername, config.getUsername())
                    .set(DataSourceConfig::getDriverClassName, config.getDriverClassName())
                    .set(DataSourceConfig::getStatus, config.getStatus())
                    .set(DataSourceConfig::getRemark, config.getRemark());
        
        if (config.getPassword() != null && !config.getPassword().trim().isEmpty() && !"******".equals(config.getPassword())) {
            try {
                String encryptedPassword = EncryptUtil.encrypt(config.getPassword());
                updateWrapper.set(DataSourceConfig::getPassword, encryptedPassword);
            } catch (Exception e) {
                return Result.error("密码加密失败");
            }
        }
        
        try {
            dataSourceConfigService.update(updateWrapper);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "删除数据源", description = "删除数据源（需确保无关联表资源）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "数据源ID") @PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigService.getById(id);
        if (config == null) {
            return Result.error("数据源不存在");
        }

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collector_table_resource WHERE datasource_id = ?", Integer.class, id);
        if (tableCount != null && tableCount > 0) {
            return Result.error("该数据源下有 " + tableCount + " 个表资源引用，请先删除关联的表资源后再删除");
        }

        try {
            dataSourceConfigService.removeById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "测试连接", description = "测试数据源连接是否可用")
    @ApiResponse(responseCode = "200", description = "测试完成")
    @PostMapping("/test-connection")
    public Result<Boolean> testConnection(@RequestBody DataSourceConfig config) {
        boolean success = dataSourceConfigService.testConnection(config);
        if (success) {
            return Result.success("连接成功", true);
        } else {
            return Result.error("连接失败");
        }
    }
    
    @Operation(summary = "扫描表列表", description = "扫描数据源中的所有表")
    @ApiResponse(responseCode = "200", description = "扫描成功")
    @GetMapping("/{id}/tables")
    public Result<List<Map<String, Object>>> scanTables(@Parameter(description = "数据源ID") @PathVariable Long id) {
        List<Map<String, Object>> tables = dataSourceConfigService.scanTables(id);
        return Result.success(tables);
    }
    
    @Operation(summary = "获取表结构", description = "获取指定表的列结构信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}/table/{tableName}/columns")
    public Result<List<Map<String, Object>>> getTableColumns(
            @Parameter(description = "数据源ID") @PathVariable Long id,
            @Parameter(description = "表名") @PathVariable String tableName) {
        List<Map<String, Object>> columns = dataSourceConfigService.getTableColumns(id, tableName);
        return Result.success(columns);
    }
    
    private String getDefaultDriver(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "dm":
                return "dm.jdbc.driver.DmDriver";
            case "kingbase":
                return "com.kingbase8.Driver";
            case "gbase":
                return "com.gbase.jdbc.Driver";
            case "oceanbase":
                return "com.oceanbase.jdbc.Driver";
            case "tidb":
                return "com.mysql.cj.jdbc.Driver";
            case "opengauss":
                return "org.opengauss.Driver";
            case "gaussdb":
                return "com.huawei.gaussdb.jdbc.Driver";
            case "highgo":
                return "com.highgo.jdbc.Driver";
            default:
                return "";
        }
    }
}
