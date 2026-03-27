package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record DemoModeStatusResponse(
        boolean demoModeEnabled,
        boolean demoItemNotesEnabled,
        boolean demoDataSeeded,
        LocalDateTime demoDataSeededAt,
        DemoDataSummaryResponse demoSummary
) {
}