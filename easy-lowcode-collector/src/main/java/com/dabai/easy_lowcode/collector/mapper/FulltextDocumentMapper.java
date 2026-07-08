package com.dabai.easy_lowcode.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dabai.easy_lowcode.collector.entity.FulltextDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FulltextDocumentMapper extends BaseMapper<FulltextDocument> {

    @Select("SELECT * FROM collector_fulltext_document WHERE deleted = 0 AND indexed = 1 AND content_text LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<FulltextDocument> searchByContentLike(String keyword, int limit);

    @Select("SELECT * FROM collector_fulltext_document WHERE deleted = 0 AND indexed = 0 LIMIT #{limit}")
    List<FulltextDocument> selectPendingIndex(int limit);
}
