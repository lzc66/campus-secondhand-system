package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.UpdateDemoModeRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.vo.admin.DemoDataSeedResponse;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/demo-mode")
public class AdminDemoModeController {

    private final AdminDemoModeService adminDemoModeService;

    public AdminDemoModeController(AdminDemoModeService adminDemoModeService) {
        this.adminDemoModeService = adminDemoModeService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping
    public ApiResponse<DemoModeStatusResponse> status() {
        return ApiResponse.success(adminDemoModeService.getStatus());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/seed")
    public ApiResponse<DemoDataSeedResponse> seed(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(adminDemoModeService.seedDemoData(principal));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PutMapping
    public ApiResponse<DemoModeStatusResponse> update(@AuthenticationPrincipal AdminPrincipal principal,
                                                      @Valid @RequestBody UpdateDemoModeRequest request) {
        return ApiResponse.success(adminDemoModeService.updateSettings(principal, request));
    }
}