package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserItemSummaryResponse(
        Long itemId,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal price,
        Integer stock,
        String status,
        String coverImageUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}