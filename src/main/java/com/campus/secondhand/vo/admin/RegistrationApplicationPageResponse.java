package com.campus.secondhand.vo.admin;

import java.util.List;

public record RegistrationApplicationPageResponse(
        long current,
        long size,
        long total,
        List<RegistrationApplicationSummaryResponse> records
) {
}