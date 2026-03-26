package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendSmtpTestEmailRequest(
        @NotBlank(message = "toEmail is required") @Email(message = "toEmail is invalid") String toEmail,
        @NotBlank(message = "subject is required") String subject,
        @NotBlank(message = "content is required") String content
) {
}