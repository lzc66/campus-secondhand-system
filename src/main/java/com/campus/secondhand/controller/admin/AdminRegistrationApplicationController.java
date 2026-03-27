package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.ReviewRegistrationApplicationRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.RegistrationReviewService;
import com.campus.secondhand.vo.admin.RegistrationApplicationDetailResponse;
import com.campus.secondhand.vo.admin.RegistrationApplicationPageResponse;
import com.campus.secondhand.vo.admin.ReviewRegistrationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/registration-applications")
public class AdminRegistrationApplicationController {

    private final RegistrationReviewService registrationReviewService;

    public AdminRegistrationApplicationController(RegistrationReviewService registrationReviewService) {
        this.registrationReviewService = registrationReviewService;
    }

    @GetMapping
    public ApiResponse<RegistrationApplicationPageResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(registrationReviewService.list(status, studentNo, email, page, size));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<RegistrationApplicationDetailResponse> detail(@PathVariable Long applicationId) {
        return ApiResponse.success(registrationReviewService.detail(applicationId));
    }

    @PostMapping("/{applicationId}/approve")
    public ApiResponse<ReviewRegistrationResponse> approve(@PathVariable Long applicationId,
                                                           @Valid @RequestBody ReviewRegistrationApplicationRequest request,
                                                           @AuthenticationPrincipal AdminPrincipal principal,
                                                           HttpServletRequest httpServletRequest) {
        return ApiResponse.success(registrationReviewService.approve(
                applicationId,
                request,
                principal,
                httpServletRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/{applicationId}/reject")
    public ApiResponse<ReviewRegistrationResponse> reject(@PathVariable Long applicationId,
                                                          @Valid @RequestBody ReviewRegistrationApplicationRequest request,
                                                          @AuthenticationPrincipal AdminPrincipal principal,
                                                          HttpServletRequest httpServletRequest) {
        return ApiResponse.success(registrationReviewService.reject(
                applicationId,
                request,
                principal,
                httpServletRequest.getRemoteAddr()
        ));
    }
}