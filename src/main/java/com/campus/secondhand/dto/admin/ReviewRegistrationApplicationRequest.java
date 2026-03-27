package com.campus.secondhand.dto.admin;

import jakarta.validation.constraints.Size;

public record ReviewRegistrationApplicationRequest(
        @Size(max = 500, message = "reviewRemark must be at most 500 characters")
        String reviewRemark
) {
}