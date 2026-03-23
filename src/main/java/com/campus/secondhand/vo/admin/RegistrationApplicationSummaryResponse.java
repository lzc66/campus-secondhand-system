package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record RegistrationApplicationSummaryResponse(
        Long applicationId,
        String applicationNo,
        String studentNo,
        String realName,
        String email,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {
}