package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.ApiManagementMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.collector.service.ApiManagementService;
import com.dabai.easy_lowcode.collector.service.DataSourceConfigService;
import com.dabai.easy_lowcode.collector.service.TableResourceService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表资源服务实现
 */
@Slf4j
@Service
public class TableResourceServiceImpl extends ServiceImpl<TableResourceMapper, TableResource> implements TableResourceService {
    
    @Autowired
    @Lazy
    private ApiManagementService apiManagementService;
    
    @Autowired
    private DataSourceConfigService dataSourceConfigService;
    
    @Autowired
    private ApiManagementMapper apiManagementMapper;
    
    /**
     * 分页查询，填充数据源名称
     */
    public Page<TableResource> pageWithDatasourceName(Page<TableResource> page, LambdaQueryWrapper<TableResource> queryWrapper) {
        // 执行分页查询
        Page<TableResource> result = super.page(page, queryWrapper);
        
        // 填充数据源名称
        fillDatasourceNames(result.getRecords());
        
        return result;
    }
    
    /**
     * 列表查询，填充数据源名称
     */
    public List<TableResource> listWithDatasourceName(LambdaQueryWrapper<TableResource> queryWrapper) {
        // 执行查询
        List<TableResource> list = super.list(queryWrapper);
        
        // 填充数据源名称
        fillDatasourceNames(list);
        
        return list;
    }
    
    /**
     * 填充数据源名称
     */
    private void fillDatasourceNames(List<TableResource> tableResources) {
        if (tableResources == null || tableResources.isEmpty()) {
            return;
        }
        
        // 收集所有数据源ID
        List<Long> datasourceIds = tableResources.stream()
            .map(TableResource::getDatasourceId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
        
        if (datasourceIds.isEmpty()) {
            return;
        }
        
        // 批量查询数据源
        List<DataSourceConfig> datasources = dataSourceConfigService.listByIds(datasourceIds);
        
        if (datasources.isEmpty()) {
            return;
        }
        
        Map<Long, String> datasourceNameMap = datasources.stream()
            .collect(Collectors.toMap(DataSourceConfig::getId, DataSourceConfig::getName));
        
        // 填充数据源名称
        tableResources.forEach(resource -> {
            if (resource.getDatasourceId() != null) {
                String name = datasourceNameMap.get(resource.getDatasourceId());
                resource.setDatasourceName(name);
            }
        });
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerTableResource(TableResource tableResource) {
        // 验证表资源数据
        validateTableResource(tableResource);
        
        // 验证数据源ID不能为空
        if (tableResource.getDatasourceId() == null) {
            throw new BusinessException("数据源ID不能为空");
        }
        
        // 验证数据源是否存在
        DataSourceConfig datasource = dataSourceConfigService.getById(tableResource.getDatasourceId());
        if (datasource == null) {
            throw new BusinessException("数据源不存在，ID: " + tableResource.getDatasourceId());
        }
        
        log.info("注册表资源: {} -> {}, 数据源ID: {}", tableResource.getTableName(), tableResource.getResourceCode(), tableResource.getDatasourceId());
        
        // 验证资源编码唯一性
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
        long count = this.count(wrapper);
        log.info("资源编码 '{}' 的存在数量: {}", tableResource.getResourceCode(), count);
        if (count > 0) {
            throw new BusinessException("资源编码已存在: " + tableResource.getResourceCode() + "，请使用其他编码");
        }
        
        // 验证API路径唯一性
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getApiPath, tableResource.getApiPath());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("API路径已存在: " + tableResource.getApiPath());
        }
        
        // 设置默认值
        if (tableResource.getMethods() == null || tableResource.getMethods().isEmpty()) {
            tableResource.setMethods("GET");
        }
        if (tableResource.getStatus() == null) {
            tableResource.setStatus(1);
        }
        
        try {
            boolean success = this.save(tableResource);
            log.info("注册表资源结果: {}, ID: {}", success ? "成功" : "失败", tableResource.getId());
            return success;
        } catch (Exception e) {
            log.error("保存表资源失败", e);
            // 如果是唯一约束冲突，提供更友好的错误信息
            if (e.getMessage() != null && e.getMessage().contains("duplicate key")) {
                if (e.getMessage().contains("uk_tr_resource_code")) {
                    throw new BusinessException("资源编码已存在: " + tableResource.getResourceCode());
                } else if (e.getMessage().contains("uk_tr_api_path")) {
                    throw new BusinessException("API路径已存在: " + tableResource.getApiPath());
                }
            }
            throw new BusinessException("注册失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean generateApi(Long resourceId) {
        TableResource resource = this.getById(resourceId);
        if (resource == null) {
            throw new BusinessException("表资源不存在");
        }
        
        // 验证资源配置完整性
        if (resource.getDatasourceId() == null) {
            throw new BusinessException("数据源ID不能为空");
        }
        
        if (resource.getTableName() == null || resource.getTableName().trim().isEmpty()) {
            throw new BusinessException("表名不能为空");
        }
        
        if (resource.getResourceCode() == null || resource.getResourceCode().trim().isEmpty()) {
            throw new BusinessException("资源编码不能为空");
        }
        
        if (resource.getApiPath() == null || resource.getApiPath().trim().isEmpty()) {
            throw new BusinessException("API路径不能为空");
        }
        
        // 注册到API管理
        log.info("生成API接口，资源ID: {}, 路径: {}", resourceId, resource.getApiPath());
        boolean success = apiManagementService.registerTableResourceApi(resourceId);
        
        if (success) {
            log.info("API生成成功，资源ID: {}", resourceId);
        } else {
            log.error("API生成失败，资源ID: {}", resourceId);
            throw new BusinessException("API生成失败");
        }
        
        return success;
    }
    
    /**
     * 验证表资源数据
     */
    private void validateTableResource(TableResource tableResource) {
        if (tableResource.getResourceCode() == null || tableResource.getResourceCode().trim().isEmpty()) {
            throw new BusinessException("资源编码不能为空");
        }
        
        if (tableResource.getApiPath() == null || tableResource.getApiPath().trim().isEmpty()) {
            throw new BusinessException("API路径不能为空");
        }
        
        // 验证API路径格式
        if (!tableResource.getApiPath().startsWith("/")) {
            throw new BusinessException("API路径必须以/开头");
        }
        
        // 验证支持的HTTP方法
        if (tableResource.getMethods() != null && !tableResource.getMethods().isEmpty()) {
            List<String> validMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");
            List<String> methods = Arrays.asList(tableResource.getMethods().split(","));
            for (String method : methods) {
                if (!validMethods.contains(method.trim().toUpperCase())) {
                    throw new BusinessException("不支持的HTTP方法: " + method);
                }
            }
        }
    }
    
    @Override
    public boolean hasRelatedApi(Long resourceId) {
        // 查询是否有API关联此表资源
        LambdaQueryWrapper<ApiManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiManagement::getSourceId, resourceId)
               .eq(ApiManagement::getApiType, "TABLE_RESOURCE");
        long count = apiManagementMapper.selectCount(wrapper);
        return count > 0;
    }
}
