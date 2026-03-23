package com.campus.secondhand.vo.admin;

import java.util.List;

public record AdminAnnouncementPageResponse(
        long current,
        long size,
        long total,
        List<AdminAnnouncementSummaryResponse> records
) {
}
