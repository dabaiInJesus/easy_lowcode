package com.dabai.easy_lowcode.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.auth.entity.SysDept;

import java.util.List;

/**
 * 部门服务接口
 */
public interface SysDeptService extends IService<SysDept> {
    
    /**
     * 获取部门树形列表
     */
    List<SysDept> getDeptTree();
}
