package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.AdminOrderActionRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminOrderManagementService;
import com.campus.secondhand.vo.admin.AdminOrderDetailResponse;
import com.campus.secondhand.vo.admin.AdminOrderPageResponse;
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
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderManagementController {

    private final AdminOrderManagementService adminOrderManagementService;

    public AdminOrderManagementController(AdminOrderManagementService adminOrderManagementService) {
        this.adminOrderManagementService = adminOrderManagementService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping
    public ApiResponse<AdminOrderPageResponse> list(@RequestParam(required = false) String orderStatus,
                                                    @RequestParam(required = false) String orderNo,
                                                    @RequestParam(required = false) String buyerStudentNo,
                                                    @RequestParam(required = false) String sellerStudentNo,
                                                    @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                    @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(adminOrderManagementService.list(orderStatus, orderNo, buyerStudentNo, sellerStudentNo, page, size));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/{orderId}")
    public ApiResponse<AdminOrderDetailResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.success(adminOrderManagementService.detail(orderId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<AdminOrderDetailResponse> cancel(@AuthenticationPrincipal AdminPrincipal principal,
                                                        @PathVariable Long orderId,
                                                        @RequestBody(required = false) AdminOrderActionRequest request) {
        return ApiResponse.success(adminOrderManagementService.cancel(principal, orderId, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{orderId}/close")
    public ApiResponse<AdminOrderDetailResponse> close(@AuthenticationPrincipal AdminPrincipal principal,
                                                       @PathVariable Long orderId,
                                                       @RequestBody(required = false) AdminOrderActionRequest request) {
        return ApiResponse.success(adminOrderManagementService.close(principal, orderId, request));
    }
}
