package com.dabai.easy_lowcode.etl.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.etl.entity.EtlDatasourceMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * ETL数据源映射 Mapper
 */
@Mapper
public interface EtlDatasourceMappingMapper extends BaseMapper<EtlDatasourceMapping> {
}
