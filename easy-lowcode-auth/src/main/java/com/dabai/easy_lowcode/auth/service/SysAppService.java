package com.dabai.easy_lowcode.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.auth.entity.SysApp;

import java.util.List;

/**
 * 应用服务接口
 */
public interface SysAppService extends IService<SysApp> {
    
    /**
     * 获取应用列表
     */
    List<SysApp> getAppList();
}
