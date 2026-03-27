package com.campus.secondhand.vo.publicapi;

public record ItemCategoryResponse(
        Long categoryId,
        Long parentId,
        String categoryCode,
        String categoryName,
        Integer sortOrder
) {
}