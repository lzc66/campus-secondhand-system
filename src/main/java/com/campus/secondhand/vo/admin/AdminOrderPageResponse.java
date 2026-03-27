package com.campus.secondhand.vo.admin;

import java.util.List;

public record AdminOrderPageResponse(
        long current,
        long size,
        long total,
        List<AdminOrderSummaryResponse> records
) {
}
