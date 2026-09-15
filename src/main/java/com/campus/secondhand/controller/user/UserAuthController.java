package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.common.util.SimpleRateLimiter;
import com.campus.secondhand.dto.user.UserLoginRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserAuthService;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import com.campus.secondhand.vo.user.UserLoginResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/auth")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @GetMapping("/captcha")
    public ApiResponse<UserCaptchaResponse> captcha(HttpServletRequest httpServletRequest) {
        ensureRateLimit(httpServletRequest, "user-captcha", 30, 60);
        return ApiResponse.success(userAuthService.getLoginCaptcha());
    }

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request,
                                                HttpServletRequest httpServletRequest) {
        ensureRateLimit(httpServletRequest, "user-login", 10, 60);
        return ApiResponse.success(userAuthService.login(
                request,
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userAuthService.getCurrentUser(principal));
    }

    private void ensureRateLimit(HttpServletRequest request, String action, int limit, int windowSeconds) {
        if (!SimpleRateLimiter.tryAcquire(action + ":" + request.getRemoteAddr(), limit, windowSeconds)) {
            throw new BusinessException(42900, HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later");
        }
    }
}