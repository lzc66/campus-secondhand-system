package com.campus.secondhand.vo.publicapi;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicItemSummaryResponse(
        Long itemId,
        Long categoryId,
        String categoryName,
        String title,
        String brand,
        String model,
        String conditionLevel,
        BigDecimal price,
        String tradeMode,
        boolean negotiable,
        String coverImageUrl,
        Integer viewCount,
        LocalDateTime publishedAt
) {
}