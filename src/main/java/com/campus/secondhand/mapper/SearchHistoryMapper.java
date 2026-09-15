package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    @Select("""
            SELECT keyword AS keyword, category_id AS categoryId, COUNT(*) AS cnt
            FROM search_histories
            WHERE searched_at >= #{start} AND searched_at < #{end}
              AND category_id IS NOT NULL
            GROUP BY keyword, category_id
            """)
    List<Map<String, Object>> selectKeywordCategoryCounts(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);
}
