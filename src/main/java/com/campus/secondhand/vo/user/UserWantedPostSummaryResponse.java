package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserWantedPostSummaryResponse(
        Long wantedPostId,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal expectedPriceMin,
        BigDecimal expectedPriceMax,
        String status,
        Integer viewCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
