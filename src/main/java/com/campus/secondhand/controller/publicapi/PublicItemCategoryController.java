package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.ItemCategoryService;
import com.campus.secondhand.vo.publicapi.ItemCategoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/item-categories")
public class PublicItemCategoryController {

    private final ItemCategoryService itemCategoryService;

    public PublicItemCategoryController(ItemCategoryService itemCategoryService) {
        this.itemCategoryService = itemCategoryService;
    }

    @GetMapping
    public ApiResponse<List<ItemCategoryResponse>> list() {
        return ApiResponse.success(itemCategoryService.listEnabledCategories());
    }
}