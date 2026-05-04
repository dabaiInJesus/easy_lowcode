package com.dabai.easy_lowcode.collector.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表资源 Mapper
 */
@Mapper
public interface TableResourceMapper extends BaseMapper<TableResource> {
    
    /**
     * 根据资源编码查询表资源
     */
    TableResource selectByResourceCode(String resourceCode);
}
