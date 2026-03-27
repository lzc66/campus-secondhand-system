package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserRecommendationResponse(
        Long recommendationId,
        Long itemId,
        Long categoryId,
        String categoryName,
        String title,
        String brand,
        String model,
        String coverImageUrl,
        BigDecimal price,
        String reasonCode,
        BigDecimal recommendScore,
        Boolean clicked,
        Integer viewCount,
        Integer commentCount,
        LocalDateTime generatedAt,
        LocalDateTime expiresAt
) {
}
