package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    @Select("""
            SELECT status AS status, COUNT(*) AS c
            FROM items
            GROUP BY status
            """)
    List<Map<String, Object>> selectStatusCounts();
}