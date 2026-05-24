package com.dabai.easy_lowcode.auth.provider;

import com.dabai.easy_lowcode.common.security.LoginUser;
import com.dabai.easy_lowcode.database.provider.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }

    @Override
    public boolean isLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof LoginUser;
    }
}