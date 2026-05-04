package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.collector.service.TableResourceService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 表资源服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableResourceServiceImpl extends ServiceImpl<TableResourceMapper, TableResource> implements TableResourceService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerTableResource(TableResource tableResource) {
        // 验证表资源数据
        validateTableResource(tableResource);
        
        // 验证资源编码唯一性
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("资源编码已存在: " + tableResource.getResourceCode());
        }
        
        // 验证API路径唯一性
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getApiPath, tableResource.getApiPath());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("API路径已存在: " + tableResource.getApiPath());
        }
        
        // 验证数据源是否存在
        // TODO: 需要注入 DataSourceConfigService 来验证数据源是否存在
        
        // 设置默认值
        if (tableResource.getMethods() == null || tableResource.getMethods().isEmpty()) {
            tableResource.setMethods("GET,POST,PUT,DELETE");
        }
        if (tableResource.getStatus() == null) {
            tableResource.setStatus(1);
        }
        
        log.info("注册表资源: {} -> {}", tableResource.getTableName(), tableResource.getResourceCode());
        return this.save(tableResource);
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
        
        // TODO: 根据资源配置动态生成API
        // 1. 解析configJson中的字段映射和验证规则
        // 2. 创建动态Controller或注册路由
        // 3. 生成对应的CRUD接口
        // 4. 注册到网关路由
        
        log.info("生成API接口，资源ID: {}, 路径: {}", resourceId, resource.getApiPath());
        
        // 标记为已生成（实际项目中可能需要一个状态字段）
        return true;
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
}
