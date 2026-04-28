package com.dabai.easy_lowcode.resource.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 资源 Mapper
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {
    
    /**
     * 根据用户ID查询资源列表
     */
    List<SysResource> selectByUserId(Long userId);
}
