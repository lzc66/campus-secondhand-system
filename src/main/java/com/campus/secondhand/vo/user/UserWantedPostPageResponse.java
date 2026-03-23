package com.campus.secondhand.vo.user;

import java.util.List;

public record UserWantedPostPageResponse(
        long current,
        long size,
        long total,
        List<UserWantedPostSummaryResponse> records
) {
}
