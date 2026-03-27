package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.SystemInitService;
import com.campus.secondhand.vo.admin.BootstrapResponse;
import com.campus.secondhand.vo.admin.InitStatusResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/init")
public class AdminInitController {

    private final SystemInitService systemInitService;

    public AdminInitController(SystemInitService systemInitService) {
        this.systemInitService = systemInitService;
    }

    @PostMapping("/bootstrap")
    public ApiResponse<BootstrapResponse> bootstrap(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(systemInitService.bootstrap(principal));
    }

    @GetMapping("/status")
    public ApiResponse<InitStatusResponse> status(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(systemInitService.getStatus(principal));
    }
}
