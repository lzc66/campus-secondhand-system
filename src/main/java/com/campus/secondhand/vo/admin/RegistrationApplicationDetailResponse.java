package com.campus.secondhand.vo.admin;

import com.campus.secondhand.vo.common.MediaFileResponse;

import java.time.LocalDateTime;

public record RegistrationApplicationDetailResponse(
        Long applicationId,
        String applicationNo,
        String studentNo,
        String realName,
        String gender,
        String email,
        String phone,
        String collegeName,
        String majorName,
        String className,
        String status,
        Long reviewerAdminId,
        String reviewRemark,
        LocalDateTime reviewedAt,
        LocalDateTime submittedAt,
        MediaFileResponse studentCardFile
) {
}