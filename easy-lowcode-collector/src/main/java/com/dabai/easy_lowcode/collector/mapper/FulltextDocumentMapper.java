package com.dabai.easy_lowcode.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dabai.easy_lowcode.collector.entity.FulltextDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FulltextDocumentMapper extends BaseMapper<FulltextDocument> {

    @Select("SELECT * FROM collector_fulltext_document WHERE deleted = 0 AND indexed = 1 AND content_text LIKE '%' || #{keyword} || '%' ORDER BY id DESC")
    List<FulltextDocument> searchByContentLike(IPage<?> page, @Param("keyword") String keyword);

    @Select("SELECT * FROM collector_fulltext_document WHERE deleted = 0 AND indexed = 0 ORDER BY id ASC")
    List<FulltextDocument> selectPendingIndex(IPage<?> page);
}
