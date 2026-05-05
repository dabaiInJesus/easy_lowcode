package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;

/**
 * API管理服务接口
 */
public interface ApiManagementService extends IService<ApiManagement> {
    
    /**
     * 注册表资源API
     * 
     * @param tableResourceId 表资源ID
     * @return 是否成功
     */
    boolean registerTableResourceApi(Long tableResourceId);
    
    /**
     * 注册外部接口API
     * 
     * @param apiManagement API管理实体
     * @return 是否成功
     */
    boolean registerExternalApi(ApiManagement apiManagement);
}
