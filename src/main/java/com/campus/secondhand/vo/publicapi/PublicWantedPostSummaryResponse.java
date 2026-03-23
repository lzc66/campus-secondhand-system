package com.campus.secondhand.vo.publicapi;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicWantedPostSummaryResponse(
        Long wantedPostId,
        Long categoryId,
        String categoryName,
        String title,
        String brand,
        String model,
        BigDecimal expectedPriceMin,
        BigDecimal expectedPriceMax,
        Integer viewCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
