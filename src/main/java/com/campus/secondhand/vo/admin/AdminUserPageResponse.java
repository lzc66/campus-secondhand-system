package com.campus.secondhand.vo.admin;

import java.util.List;

public record AdminUserPageResponse(
        long current,
        long size,
        long total,
        List<AdminUserSummaryResponse> records
) {
}
