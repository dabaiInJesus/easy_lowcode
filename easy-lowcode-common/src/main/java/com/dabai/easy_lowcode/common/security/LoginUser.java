package com.dabai.easy_lowcode.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class LoginUser extends User {
    
    private final Long userId;
    
    public LoginUser(Long userId, String username, String password, 
                     boolean enabled, boolean accountNonExpired, 
                     boolean credentialsNonExpired, boolean accountNonLocked,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, 
              credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
    }
}