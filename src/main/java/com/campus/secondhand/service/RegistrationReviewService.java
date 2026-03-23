package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.ReviewRegistrationApplicationRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.RegistrationApplicationDetailResponse;
import com.campus.secondhand.vo.admin.RegistrationApplicationPageResponse;
import com.campus.secondhand.vo.admin.ReviewRegistrationResponse;

public interface RegistrationReviewService {

    RegistrationApplicationPageResponse list(String status, String studentNo, String email, long page, long size);

    RegistrationApplicationDetailResponse detail(Long applicationId);

    ReviewRegistrationResponse approve(Long applicationId, ReviewRegistrationApplicationRequest request, AdminPrincipal principal, String ipAddress);

    ReviewRegistrationResponse reject(Long applicationId, ReviewRegistrationApplicationRequest request, AdminPrincipal principal, String ipAddress);
}