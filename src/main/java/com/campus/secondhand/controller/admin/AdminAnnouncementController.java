package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.admin.AnnouncementActionRequest;
import com.campus.secondhand.dto.admin.SaveAnnouncementRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.service.AdminAnnouncementService;
import com.campus.secondhand.vo.admin.AdminAnnouncementDetailResponse;
import com.campus.secondhand.vo.admin.AdminAnnouncementPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/announcements")
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    public AdminAnnouncementController(AdminAnnouncementService adminAnnouncementService) {
        this.adminAnnouncementService = adminAnnouncementService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR','AUDITOR')")
    @GetMapping
    public ApiResponse<AdminAnnouncementPageResponse> list(@RequestParam(required = false) String publishStatus,
                                                           @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                           @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(adminAnnouncementService.list(publishStatus, page, size));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR','AUDITOR')")
    @GetMapping("/{announcementId}")
    public ApiResponse<AdminAnnouncementDetailResponse> detail(@PathVariable Long announcementId) {
        return ApiResponse.success(adminAnnouncementService.detail(announcementId));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping
    public ApiResponse<AdminAnnouncementDetailResponse> create(@AuthenticationPrincipal AdminPrincipal principal,
                                                               @Valid @RequestBody SaveAnnouncementRequest request) {
        return ApiResponse.success(adminAnnouncementService.create(principal, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PutMapping("/{announcementId}")
    public ApiResponse<AdminAnnouncementDetailResponse> update(@AuthenticationPrincipal AdminPrincipal principal,
                                                               @PathVariable Long announcementId,
                                                               @Valid @RequestBody SaveAnnouncementRequest request) {
        return ApiResponse.success(adminAnnouncementService.update(principal, announcementId, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{announcementId}/publish")
    public ApiResponse<AdminAnnouncementDetailResponse> publish(@AuthenticationPrincipal AdminPrincipal principal,
                                                                @PathVariable Long announcementId,
                                                                @RequestBody(required = false) AnnouncementActionRequest request) {
        return ApiResponse.success(adminAnnouncementService.publish(principal, announcementId, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OPERATOR')")
    @PostMapping("/{announcementId}/offline")
    public ApiResponse<AdminAnnouncementDetailResponse> offline(@AuthenticationPrincipal AdminPrincipal principal,
                                                                @PathVariable Long announcementId,
                                                                @RequestBody(required = false) AnnouncementActionRequest request) {
        return ApiResponse.success(adminAnnouncementService.offline(principal, announcementId, request));
    }
}
