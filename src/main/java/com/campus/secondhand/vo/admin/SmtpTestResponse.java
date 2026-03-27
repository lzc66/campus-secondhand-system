package com.campus.secondhand.vo.admin;

import java.time.LocalDateTime;

public record SmtpTestResponse(
        String toEmail,
        String subject,
        LocalDateTime testedAt
) {
}