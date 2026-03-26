package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.AdminLoginRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminAuthService;
import com.campus.secondhand.vo.admin.AdminLoginResponse;
import com.campus.secondhand.vo.admin.AdminProfileResponse;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/captcha")
    public ApiResponse<UserCaptchaResponse> captcha() {
        return ApiResponse.success(adminAuthService.getLoginCaptcha());
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                 HttpServletRequest httpServletRequest) {
        return ApiResponse.success(adminAuthService.login(
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(adminAuthService.getCurrentAdmin(principal));
    }
}