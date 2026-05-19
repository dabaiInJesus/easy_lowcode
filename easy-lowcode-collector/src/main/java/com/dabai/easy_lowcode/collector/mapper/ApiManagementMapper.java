package com.dabai.easy_lowcode.collector.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 * API管理 Mapper
 */
@Mapper
public interface ApiManagementMapper extends BaseMapper<ApiManagement> {
    
    /**
     * 根据API路径和方法查询
     */
    ApiManagement selectByPathAndMethod(@Param("apiPath") String apiPath, @Param("apiMethod") String apiMethod);
    
}
