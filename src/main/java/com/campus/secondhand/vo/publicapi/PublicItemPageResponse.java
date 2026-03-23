package com.campus.secondhand.vo.publicapi;

import java.util.List;

public record PublicItemPageResponse(
        long current,
        long size,
        long total,
        List<PublicItemSummaryResponse> records
) {
}