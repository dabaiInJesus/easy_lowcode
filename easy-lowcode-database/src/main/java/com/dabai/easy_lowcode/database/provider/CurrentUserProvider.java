package com.dabai.easy_lowcode.database.provider;

/**
 * 当前用户提供者接口
 * 用于解耦 database 模块与认证框架（Sa-Token/Spring Security 等）
 */
public interface CurrentUserProvider {

    /**
     * 获取当前登录用户ID
     * @return 用户ID，未登录时返回 null
     */
    Long getCurrentUserId();

    /**
     * 判断当前用户是否已登录
     * @return true 如果已登录
     */
    boolean isLogin();
}
