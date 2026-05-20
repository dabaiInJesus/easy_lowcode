package com.dabai.easy_lowcode.auth.provider;

import cn.dev33.satoken.stp.StpUtil;
import com.dabai.easy_lowcode.database.provider.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的当前用户提供者实现
 */
@Component
public class SaTokenCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }
}
