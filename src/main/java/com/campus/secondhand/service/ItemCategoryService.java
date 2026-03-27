package com.campus.secondhand.service;

import com.campus.secondhand.vo.publicapi.ItemCategoryResponse;

import java.util.List;

public interface ItemCategoryService {

    List<ItemCategoryResponse> listEnabledCategories();
}