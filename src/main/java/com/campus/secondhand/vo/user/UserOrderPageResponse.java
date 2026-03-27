package com.campus.secondhand.vo.user;

import java.util.List;

public record UserOrderPageResponse(
        long current,
        long size,
        long total,
        List<UserOrderSummaryResponse> records
) {
}
