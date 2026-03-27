package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.PublicAnnouncementService;
import com.campus.secondhand.vo.publicapi.PublicAnnouncementPageResponse;
import com.campus.secondhand.vo.publicapi.PublicAnnouncementResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/announcements")
public class PublicAnnouncementController {

    private final PublicAnnouncementService publicAnnouncementService;

    public PublicAnnouncementController(PublicAnnouncementService publicAnnouncementService) {
        this.publicAnnouncementService = publicAnnouncementService;
    }

    @GetMapping
    public ApiResponse<PublicAnnouncementPageResponse> list(@RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(publicAnnouncementService.listPublished(page, size));
    }

    @GetMapping("/{announcementId}")
    public ApiResponse<PublicAnnouncementResponse> detail(@PathVariable Long announcementId) {
        return ApiResponse.success(publicAnnouncementService.detail(announcementId));
    }
}
