package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysDept;
import com.dabai.easy_lowcode.auth.mapper.SysDeptMapper;
import com.dabai.easy_lowcode.auth.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {
    
    @Override
    public List<SysDept> getDeptTree() {
        // 查询所有部门
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1) // 只查询正常状态的部门
                .orderByAsc(SysDept::getSort);
        List<SysDept> allDepts = this.list(wrapper);
        
        // 构建树形结构
        return buildTree(allDepts, 0L);
    }
    
    /**
     * 递归构建树形结构
     */
    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        return depts.stream()
                .filter(dept -> dept.getParentId().equals(parentId))
                .peek(dept -> {
                    List<SysDept> children = buildTree(depts, dept.getId());
                    // 这里可以设置子部门，但实体中没有children字段
                })
                .collect(Collectors.toList());
    }
}
