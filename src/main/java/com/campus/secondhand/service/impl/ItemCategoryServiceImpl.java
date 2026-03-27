package com.campus.secondhand.service.impl;

import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.service.ItemCategoryService;
import com.campus.secondhand.vo.publicapi.ItemCategoryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemCategoryServiceImpl implements ItemCategoryService {

    private final ItemCategoryMapper itemCategoryMapper;

    public ItemCategoryServiceImpl(ItemCategoryMapper itemCategoryMapper) {
        this.itemCategoryMapper = itemCategoryMapper;
    }

    @Override
    public List<ItemCategoryResponse> listEnabledCategories() {
        return itemCategoryMapper.selectEnabledCategories().stream()
                .map(item -> new ItemCategoryResponse(
                        item.getCategoryId(),
                        item.getParentId(),
                        item.getCategoryCode(),
                        item.getCategoryName(),
                        item.getSortOrder()
                ))
                .toList();
    }
}