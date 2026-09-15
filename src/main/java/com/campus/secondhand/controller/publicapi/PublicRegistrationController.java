package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.common.util.SimpleRateLimiter;
import com.campus.secondhand.dto.publicapi.RegistrationApplicationSubmitRequest;
import com.campus.secondhand.service.PublicRegistrationService;
import com.campus.secondhand.vo.publicapi.RegistrationApplicationSubmitResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/registration-applications")
public class PublicRegistrationController {

    private final PublicRegistrationService publicRegistrationService;

    public PublicRegistrationController(PublicRegistrationService publicRegistrationService) {
        this.publicRegistrationService = publicRegistrationService;
    }

    @PostMapping
    public ApiResponse<RegistrationApplicationSubmitResponse> submit(@Valid @RequestBody RegistrationApplicationSubmitRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        // 匿名注册提交按 IP 限频,防止批量刷注册申请
        if (!SimpleRateLimiter.tryAcquire("registration-submit:" + httpServletRequest.getRemoteAddr(), 5, 60)) {
            throw new BusinessException(42900, HttpStatus.TOO_MANY_REQUESTS, "Too many registration attempts, please try again later");
        }
        return ApiResponse.success(publicRegistrationService.submit(request));
    }
}