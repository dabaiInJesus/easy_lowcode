package com.dabai.easy_lowcode.etl.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.etl.entity.EtlTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * ETL任务日志 Mapper
 */
@Mapper
public interface EtlTaskLogMapper extends BaseMapper<EtlTaskLog> {
}
