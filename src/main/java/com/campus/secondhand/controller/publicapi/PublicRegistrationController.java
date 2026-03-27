package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.publicapi.RegistrationApplicationSubmitRequest;
import com.campus.secondhand.service.PublicRegistrationService;
import com.campus.secondhand.vo.publicapi.RegistrationApplicationSubmitResponse;
import jakarta.validation.Valid;
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
    public ApiResponse<RegistrationApplicationSubmitResponse> submit(@Valid @RequestBody RegistrationApplicationSubmitRequest request) {
        return ApiResponse.success(publicRegistrationService.submit(request));
    }
}