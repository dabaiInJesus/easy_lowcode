package com.dabai.easy_lowcode.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_resource")
public class SysRoleResource {
    private Long id;
    private Long roleId;
    private Long resourceId;
}
