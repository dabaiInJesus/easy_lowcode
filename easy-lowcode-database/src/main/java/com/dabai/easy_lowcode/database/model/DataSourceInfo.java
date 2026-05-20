package com.dabai.easy_lowcode.database.model;

import lombok.Data;

/**
 * 数据源信息 DTO
 * 用于跨模块传递数据源配置，避免直接依赖 collector 模块
 */
@Data
public class DataSourceInfo {

    private Long id;
    private String name;
    private String code;
    private String dbType;
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Integer status;
    private String remark;
    private String extraConfig;

    /**
     * 获取解密后的密码
     */
    public String getDecryptedPassword() {
        return password;
    }
}
