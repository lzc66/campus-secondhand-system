package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.common.util.SimpleRateLimiter;
import com.campus.secondhand.dto.admin.AdminChangePasswordRequest;
import com.campus.secondhand.dto.admin.AdminLoginRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminAuthService;
import com.campus.secondhand.vo.admin.AdminLoginResponse;
import com.campus.secondhand.vo.admin.AdminProfileResponse;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ApiResponse<UserCaptchaResponse> captcha(HttpServletRequest httpServletRequest) {
        ensureRateLimit(httpServletRequest, "admin-captcha", 30, 60);
        return ApiResponse.success(adminAuthService.getLoginCaptcha());
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                 HttpServletRequest httpServletRequest) {
        // 管理员账号不做"失败禁用"(会被恶意爆破打成 DoS),以 IP 限频 + 验证码防护
        ensureRateLimit(httpServletRequest, "admin-login", 10, 60);
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

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AdminPrincipal principal,
                                            @Valid @RequestBody AdminChangePasswordRequest request) {
        adminAuthService.changePassword(principal, request);
        return ApiResponse.success("Password updated", (Void) null);
    }

    private void ensureRateLimit(HttpServletRequest request, String action, int limit, int windowSeconds) {
        if (!SimpleRateLimiter.tryAcquire(action + ":" + request.getRemoteAddr(), limit, windowSeconds)) {
            throw new BusinessException(42900, HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later");
        }
    }
}