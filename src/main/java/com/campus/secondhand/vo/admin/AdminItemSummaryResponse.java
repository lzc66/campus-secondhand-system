package com.campus.secondhand.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminItemSummaryResponse(
        Long itemId,
        Long sellerUserId,
        String sellerStudentNo,
        String sellerName,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal price,
        Integer stock,
        String status,
        Integer viewCount,
        Integer commentCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
}
