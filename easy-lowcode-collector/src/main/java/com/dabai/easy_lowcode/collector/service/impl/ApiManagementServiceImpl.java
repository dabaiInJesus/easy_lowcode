package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.ApiManagementMapper;
import com.dabai.easy_lowcode.collector.service.ApiManagementService;
import com.dabai.easy_lowcode.collector.service.TableResourceService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiManagementServiceImpl extends ServiceImpl<ApiManagementMapper, ApiManagement> implements ApiManagementService {
    
    private final TableResourceService tableResourceService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerTableResourceApi(Long tableResourceId) {
        log.info("注册表资源API，表资源ID: {}", tableResourceId);
        
        // 查询表资源
        TableResource tableResource = tableResourceService.getById(tableResourceId);
        if (tableResource == null) {
            throw new BusinessException("表资源不存在");
        }
        
        // 检查是否已存在相同的API
        ApiManagement existingApi = baseMapper.selectByPathAndMethod(
            tableResource.getApiPath(), 
            "GET"
        );
        
        if (existingApi != null) {
            log.warn("API路径已存在: {} GET", tableResource.getApiPath());
            throw new BusinessException("API路径已存在: " + tableResource.getApiPath() + " GET");
        }
        
        // 创建API管理记录
        ApiManagement apiManagement = new ApiManagement();
        apiManagement.setApiName(tableResource.getTableComment() != null ? 
            tableResource.getTableComment() : tableResource.getTableName());
        apiManagement.setApiPath(tableResource.getApiPath());
        apiManagement.setApiMethod("GET");
        apiManagement.setApiType("TABLE_RESOURCE");
        apiManagement.setSourceId(tableResourceId);
        apiManagement.setDescription("表资源API - " + tableResource.getTableName());
        apiManagement.setStatus(tableResource.getStatus());
        apiManagement.setVersion("v1");
            apiManagement.setAuthRequired(false);
        
        // 保存
        boolean success = this.save(apiManagement);
        
        if (success) {
            log.info("注册表资源API成功，API ID: {}, 路径: {}", apiManagement.getId(), tableResource.getApiPath());
        } else {
            log.error("注册表资源API失败，表资源ID: {}", tableResourceId);
            throw new BusinessException("注册API失败");
        }
        
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerExternalApi(ApiManagement apiManagement) {
        log.info("注册外部接口API: {}", apiManagement.getApiName());
        
        // 验证必填字段
        if (apiManagement.getApiName() == null || apiManagement.getApiName().trim().isEmpty()) {
            throw new BusinessException("API名称不能为空");
        }
        
        if (apiManagement.getApiPath() == null || apiManagement.getApiPath().trim().isEmpty()) {
            throw new BusinessException("API路径不能为空");
        }
        
        if (apiManagement.getApiMethod() == null || apiManagement.getApiMethod().trim().isEmpty()) {
            throw new BusinessException("HTTP方法不能为空");
        }
        
        // 检查是否已存在相同的API
        ApiManagement existingApi = baseMapper.selectByPathAndMethod(
            apiManagement.getApiPath(), 
            apiManagement.getApiMethod()
        );
        
        if (existingApi != null) {
            log.warn("API路径已存在: {} {}", apiManagement.getApiPath(), apiManagement.getApiMethod());
            throw new BusinessException("API路径已存在: " + apiManagement.getApiPath() + " " + apiManagement.getApiMethod());
        }
        
        // 设置默认值
        if (apiManagement.getApiType() == null) {
            apiManagement.setApiType("EXTERNAL");
        }
        
        if (apiManagement.getStatus() == null) {
            apiManagement.setStatus(1);
        }
        
        if (apiManagement.getVersion() == null) {
            apiManagement.setVersion("v1");
        }
        
        if (apiManagement.getAuthRequired() == null) {
        apiManagement.setAuthRequired(false);
        }
        
        if (apiManagement.getSortOrder() == null) {
            apiManagement.setSortOrder(0);
        }
        
        // 保存
        boolean success = this.save(apiManagement);
        
        if (success) {
            log.info("注册外部接口API成功，API ID: {}, 路径: {}", apiManagement.getId(), apiManagement.getApiPath());
        } else {
            log.error("注册外部接口API失败: {}", apiManagement.getApiName());
            throw new BusinessException("注册API失败");
        }
        
        return success;
    }
}
