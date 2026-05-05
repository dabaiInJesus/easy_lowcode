package com.dabai.easy_lowcode.etl.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * ETL任务 Mapper
 */
@Mapper
public interface EtlTaskMapper extends BaseMapper<EtlTask> {
}
