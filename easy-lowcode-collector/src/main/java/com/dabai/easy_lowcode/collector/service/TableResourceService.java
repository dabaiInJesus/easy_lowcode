package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.TableResource;

import java.util.List;

/**
 * 表资源服务接口
 */
public interface TableResourceService extends IService<TableResource> {
    
    /**
     * 注册表资源
     */
    boolean registerTableResource(TableResource tableResource);
    
    /**
     * 生成API接口
     */
    boolean generateApi(Long resourceId);
    
    /**
     * 检查表资源是否有关联的API
     */
    boolean hasRelatedApi(Long resourceId);
    
    /**
     * 分页查询，填充数据源名称
     */
    Page<TableResource> pageWithDatasourceName(Page<TableResource> page, LambdaQueryWrapper<TableResource> queryWrapper);
    
    /**
     * 列表查询，填充数据源名称
     */
    List<TableResource> listWithDatasourceName(LambdaQueryWrapper<TableResource> queryWrapper);
}
