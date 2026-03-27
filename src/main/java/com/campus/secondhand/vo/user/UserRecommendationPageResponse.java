package com.campus.secondhand.vo.user;

import java.util.List;

public record UserRecommendationPageResponse(
        long current,
        long size,
        long total,
        List<UserRecommendationResponse> records
) {
}
