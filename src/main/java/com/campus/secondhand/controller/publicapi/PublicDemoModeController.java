package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/demo-mode")
public class PublicDemoModeController {

    private final AdminDemoModeService adminDemoModeService;

    public PublicDemoModeController(AdminDemoModeService adminDemoModeService) {
        this.adminDemoModeService = adminDemoModeService;
    }

    @GetMapping
    public ApiResponse<DemoModeStatusResponse> status() {
        return ApiResponse.success(adminDemoModeService.getStatus());
    }
}