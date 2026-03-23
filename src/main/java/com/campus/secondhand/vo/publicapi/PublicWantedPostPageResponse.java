package com.campus.secondhand.vo.publicapi;

import java.util.List;

public record PublicWantedPostPageResponse(
        long current,
        long size,
        long total,
        List<PublicWantedPostSummaryResponse> records
) {
}
