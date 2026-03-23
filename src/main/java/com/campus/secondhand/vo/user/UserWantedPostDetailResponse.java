package com.campus.secondhand.vo.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserWantedPostDetailResponse(
        Long wantedPostId,
        Long categoryId,
        String categoryName,
        String title,
        String brand,
        String model,
        String description,
        BigDecimal expectedPriceMin,
        BigDecimal expectedPriceMax,
        String contactPhone,
        String contactQq,
        String contactWechat,
        String status,
        Integer viewCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
