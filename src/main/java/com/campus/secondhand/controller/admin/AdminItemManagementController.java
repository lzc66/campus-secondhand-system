package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.UpdateItemStatusRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminItemManagementService;
import com.campus.secondhand.vo.admin.AdminItemDetailResponse;
import com.campus.secondhand.vo.admin.AdminItemPageResponse;
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
@RequestMapping("/api/v1/admin/items")
public class AdminItemManagementController {

    private final AdminItemManagementService adminItemManagementService;

    public AdminItemManagementController(AdminItemManagementService adminItemManagementService) {
        this.adminItemManagementService = adminItemManagementService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping
    public ApiResponse<AdminItemPageResponse> list(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String sellerStudentNo,
                                                   @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                   @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(adminItemManagementService.list(status, categoryId, keyword, sellerStudentNo, page, size));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/{itemId}")
    public ApiResponse<AdminItemDetailResponse> detail(@PathVariable Long itemId) {
        return ApiResponse.success(adminItemManagementService.detail(itemId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{itemId}/status")
    public ApiResponse<AdminItemDetailResponse> updateStatus(@AuthenticationPrincipal AdminPrincipal principal,
                                                             @PathVariable Long itemId,
                                                             @Valid @RequestBody UpdateItemStatusRequest request) {
        return ApiResponse.success(adminItemManagementService.updateStatus(principal, itemId, request));
    }
}
