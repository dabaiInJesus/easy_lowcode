package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.service.DataSourceConfigService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/collector/datasource")
@RequiredArgsConstructor
public class DataSourceConfigController {
    
    private final DataSourceConfigService dataSourceConfigService;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 分页查询数据源列表
     */
    @GetMapping("/page")
    public Result<PageResult<DataSourceConfig>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(DataSourceConfig::getName, keyword)
                   .or()
                   .like(DataSourceConfig::getCode, keyword);
        }
        wrapper.orderByDesc(DataSourceConfig::getCreateTime);
        
        Page<DataSourceConfig> page = dataSourceConfigService.page(new Page<>(current, size), wrapper);
        
        // 不返回密码
        page.getRecords().forEach(ds -> ds.setPassword("******"));
        
        PageResult<DataSourceConfig> result = new PageResult<>(
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getRecords()
        );
        
        return Result.success(result);
    }
    
    /**
     * 获取数据源详情
     */
    @GetMapping("/{id}")
    public Result<DataSourceConfig> getById(@PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigService.getById(id);
        // 返回真实的加密密码，用于测试连接等操作
        return Result.success(config);
    }
    
    /**
     * 创建数据源
     */
    @PostMapping
    public Result<Void> create(@RequestBody DataSourceConfig config) {
        // 参数验证
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
        
        // 检查编码唯一性
        LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSourceConfig::getCode, config.getCode());
        if (dataSourceConfigService.count(wrapper) > 0) {
            return Result.error("数据源编码已存在: " + config.getCode());
        }
        
        // 加密密码
        try {
            config.setPassword(EncryptUtil.encrypt(config.getPassword()));
        } catch (Exception e) {
            log.error("密码加密失败", e);
            return Result.error("密码加密失败");
        }
        
        // 设置默认驱动
        if (config.getDriverClassName() == null || config.getDriverClassName().isEmpty()) {
            String defaultDriver = getDefaultDriver(config.getDbType());
            if (defaultDriver.isEmpty()) {
                return Result.error("不支持的数据库类型: " + config.getDbType());
            }
            config.setDriverClassName(defaultDriver);
        }
        
        // 设置默认状态
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        
        try {
            dataSourceConfigService.save(config);
            log.info("创建数据源成功: {}", config.getName());
            return Result.success("创建成功");
        } catch (Exception e) {
            log.error("创建数据源失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新数据源
     */
    @PutMapping
    public Result<Void> update(@RequestBody DataSourceConfig config) {
        if (config.getId() == null) {
            return Result.error("数据源ID不能为空");
        }
        
        // 检查数据源是否存在
        DataSourceConfig existing = dataSourceConfigService.getById(config.getId());
        if (existing == null) {
            return Result.error("数据源不存在");
        }
        
        // 如果修改了编码，检查唯一性
        if (config.getCode() != null && !config.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<DataSourceConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataSourceConfig::getCode, config.getCode());
            wrapper.ne(DataSourceConfig::getId, config.getId());
            if (dataSourceConfigService.count(wrapper) > 0) {
                return Result.error("数据源编码已存在: " + config.getCode());
            }
        }
        
        // 构建更新条件
        LambdaUpdateWrapper<DataSourceConfig> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DataSourceConfig::getId, config.getId());
        
        // 设置要更新的字段
        updateWrapper.set(DataSourceConfig::getName, config.getName())
                    .set(DataSourceConfig::getCode, config.getCode())
                    .set(DataSourceConfig::getDbType, config.getDbType())
                    .set(DataSourceConfig::getUrl, config.getUrl())
                    .set(DataSourceConfig::getUsername, config.getUsername())
                    .set(DataSourceConfig::getDriverClassName, config.getDriverClassName())
                    .set(DataSourceConfig::getStatus, config.getStatus())
                    .set(DataSourceConfig::getRemark, config.getRemark());
        
        // 处理密码：只有当提供了新密码时才更新
        if (config.getPassword() != null && !config.getPassword().trim().isEmpty() && !"******".equals(config.getPassword())) {
            try {
                String encryptedPassword = EncryptUtil.encrypt(config.getPassword());
                updateWrapper.set(DataSourceConfig::getPassword, encryptedPassword);
                log.debug("更新密码");
            } catch (Exception e) {
                log.error("密码加密失败", e);
                return Result.error("密码加密失败");
            }
        } else {
            log.debug("保持原密码不变");
        }
        
        try {
            dataSourceConfigService.update(updateWrapper);
            log.info("更新数据源成功: {}", config.getId());
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新数据源失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除数据源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
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
            log.info("删除数据源成功: {}", id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除数据源失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/test-connection")
    public Result<Boolean> testConnection(@RequestBody DataSourceConfig config) {
        boolean success = dataSourceConfigService.testConnection(config);
        if (success) {
            return Result.success("连接成功", true);
        } else {
            return Result.error("连接失败");
        }
    }
    
    /**
     * 扫描表列表
     */
    @GetMapping("/{id}/tables")
    public Result<List<Map<String, Object>>> scanTables(@PathVariable Long id) {
        List<Map<String, Object>> tables = dataSourceConfigService.scanTables(id);
        return Result.success(tables);
    }
    
    /**
     * 获取表结构
     */
    @GetMapping("/{id}/table/{tableName}/columns")
    public Result<List<Map<String, Object>>> getTableColumns(
            @PathVariable Long id,
            @PathVariable String tableName) {
        List<Map<String, Object>> columns = dataSourceConfigService.getTableColumns(id, tableName);
        return Result.success(columns);
    }
    
    /**
     * 根据数据库类型获取默认驱动
     */
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
            // 国产数据库
            case "dm": // 达梦数据库
                return "dm.jdbc.driver.DmDriver";
            case "kingbase": // 人大金仓
                return "com.kingbase8.Driver";
            case "gbase": // 南大通用GBase 8a/8s
                return "com.gbase.jdbc.Driver";
            case "oceanbase": // OceanBase
                return "com.oceanbase.jdbc.Driver";
            case "tidb": // TiDB（兼容MySQL协议）
                return "com.mysql.cj.jdbc.Driver";
            case "opengauss": // openGauss
                return "org.opengauss.Driver";
            case "gaussdb": // 华为GaussDB
                return "com.huawei.gaussdb.jdbc.Driver";
            case "highgo": // 瀚高数据库
                return "com.highgo.jdbc.Driver";
            default:
                return "";
        }
    }
}
