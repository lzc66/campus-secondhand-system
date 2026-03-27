package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.UpdateUserStatusRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminUserManagementService;
import com.campus.secondhand.vo.admin.AdminUserDetailResponse;
import com.campus.secondhand.vo.admin.AdminUserPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/users")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping
    public ApiResponse<AdminUserPageResponse> list(@RequestParam(required = false) String accountStatus,
                                                   @RequestParam(required = false) String studentNo,
                                                   @RequestParam(required = false) String realName,
                                                   @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                   @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(adminUserManagementService.list(accountStatus, studentNo, realName, page, size));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailResponse> detail(@PathVariable Long userId) {
        return ApiResponse.success(adminUserManagementService.detail(userId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{userId}/status")
    public ApiResponse<AdminUserDetailResponse> updateStatus(@AuthenticationPrincipal AdminPrincipal principal,
                                                             @PathVariable Long userId,
                                                             @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success(adminUserManagementService.updateStatus(principal, userId, request));
    }
}
