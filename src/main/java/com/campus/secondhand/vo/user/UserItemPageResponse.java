package com.campus.secondhand.vo.user;

import java.util.List;

public record UserItemPageResponse(
        long current,
        long size,
        long total,
        List<UserItemSummaryResponse> records
) {
}