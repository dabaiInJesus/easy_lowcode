package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysApp;
import com.dabai.easy_lowcode.auth.mapper.SysAppMapper;
import com.dabai.easy_lowcode.auth.service.SysAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAppServiceImpl extends ServiceImpl<SysAppMapper, SysApp> implements SysAppService {
    
    @Override
    public List<SysApp> getAppList() {
        LambdaQueryWrapper<SysApp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysApp::getStatus, 1) // 只查询正常状态的应用
                .orderByAsc(SysApp::getSort);
        return this.list(wrapper);
    }
}
