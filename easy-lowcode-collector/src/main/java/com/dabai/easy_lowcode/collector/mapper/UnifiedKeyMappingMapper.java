package com.dabai.easy_lowcode.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dabai.easy_lowcode.collector.entity.UnifiedKeyMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UnifiedKeyMappingMapper extends BaseMapper<UnifiedKeyMapping> {

    @Select("SELECT DISTINCT unified_key, display_name FROM collector_unified_key_mapping WHERE deleted = 0 ORDER BY unified_key")
    List<UnifiedKeyMapping> selectDistinctKeys();

    @Select("SELECT * FROM collector_unified_key_mapping WHERE unified_key = #{unifiedKey} AND deleted = 0 ORDER BY sort_order")
    List<UnifiedKeyMapping> selectByUnifiedKey(String unifiedKey);

    @Select("SELECT * FROM collector_unified_key_mapping WHERE resource_code = #{resourceCode} AND deleted = 0")
    List<UnifiedKeyMapping> selectByResourceCode(String resourceCode);
}
