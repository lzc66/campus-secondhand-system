package com.campus.secondhand.vo.admin;

public record AdminDashboardHotKeywordResponse(
        String keyword,
        long searchCount,
        Long categoryId,
        String categoryName
) {
}