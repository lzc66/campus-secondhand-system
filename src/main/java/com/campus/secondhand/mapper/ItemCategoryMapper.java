package com.campus.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.secondhand.entity.ItemCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemCategoryMapper extends BaseMapper<ItemCategory> {

    @Select("SELECT * FROM item_categories WHERE category_code = #{categoryCode} LIMIT 1")
    ItemCategory selectByCategoryCode(@Param("categoryCode") String categoryCode);

    @Select("SELECT * FROM item_categories WHERE is_enabled = 1 ORDER BY sort_order ASC, category_id ASC")
    List<ItemCategory> selectEnabledCategories();
}