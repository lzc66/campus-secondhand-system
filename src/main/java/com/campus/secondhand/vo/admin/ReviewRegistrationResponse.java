package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record ReviewRegistrationResponse(
        Long applicationId,
        String status,
        Long userId,
        LocalDateTime reviewedAt
) {
}