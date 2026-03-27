package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.SendSmtpTestEmailRequest;
import com.campus.secondhand.dto.admin.UpdateSmtpSettingsRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.SmtpSettingsService;
import com.campus.secondhand.vo.admin.SmtpSettingsResponse;
import com.campus.secondhand.vo.admin.SmtpTestResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system/smtp")
public class AdminSmtpSettingsController {

    private final SmtpSettingsService smtpSettingsService;

    public AdminSmtpSettingsController(SmtpSettingsService smtpSettingsService) {
        this.smtpSettingsService = smtpSettingsService;
    }

    @GetMapping
    public ApiResponse<SmtpSettingsResponse> getSettings() {
        return ApiResponse.success(smtpSettingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<SmtpSettingsResponse> updateSettings(@Valid @RequestBody UpdateSmtpSettingsRequest request,
                                                            @AuthenticationPrincipal AdminPrincipal principal,
                                                            HttpServletRequest httpServletRequest) {
        return ApiResponse.success(smtpSettingsService.updateSettings(request, principal, httpServletRequest.getRemoteAddr()));
    }

    @PostMapping("/test")
    public ApiResponse<SmtpTestResponse> sendTest(@Valid @RequestBody SendSmtpTestEmailRequest request,
                                                  @AuthenticationPrincipal AdminPrincipal principal,
                                                  HttpServletRequest httpServletRequest) {
        return ApiResponse.success(smtpSettingsService.sendTestEmail(request, principal, httpServletRequest.getRemoteAddr()));
    }
}