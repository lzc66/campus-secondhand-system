package com.campus.secondhand.vo.publicapi;

public record RegistrationApplicationSubmitResponse(
        Long applicationId,
        String applicationNo,
        String status
) {
}