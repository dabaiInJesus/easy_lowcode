package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.TableResource;

/**
 * 表资源服务接口
 */
public interface TableResourceService extends IService<TableResource> {
    
    /**
     * 注册表资源
     * 
     * @param tableResource 表资源配置
     * @return 是否成功
     */
    boolean registerTableResource(TableResource tableResource);
    
    /**
     * 生成API接口
     * 
     * @param resourceId 资源ID
     * @return 是否成功
     */
    boolean generateApi(Long resourceId);
    
    /**
     * 检查表资源是否有关联的API
     * 
     * @param resourceId 表资源ID
     * @return 是否有关联API
     */
    boolean hasRelatedApi(Long resourceId);
}
