package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record DemoDataSeedResponse(
        DemoDataSummaryResponse createdCounts,
        DemoDataSummaryResponse totalCounts,
        boolean demoDataSeeded,
        LocalDateTime demoDataSeededAt
) {
}