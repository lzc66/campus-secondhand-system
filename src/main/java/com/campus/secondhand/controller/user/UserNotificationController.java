package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserNotificationCenterService;
import com.campus.secondhand.vo.user.UserNotificationPageResponse;
import com.campus.secondhand.vo.user.UserNotificationResponse;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/user/notifications")
public class UserNotificationController {

    private final UserNotificationCenterService userNotificationCenterService;

    public UserNotificationController(UserNotificationCenterService userNotificationCenterService) {
        this.userNotificationCenterService = userNotificationCenterService;
    }

    @GetMapping
    public ApiResponse<UserNotificationPageResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam(required = false) String readStatus,
                                                          @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                          @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(userNotificationCenterService.listNotifications(principal, readStatus, page, size));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<UserNotificationResponse> markRead(@AuthenticationPrincipal UserPrincipal principal,
                                                          @PathVariable Long notificationId) {
        return ApiResponse.success(userNotificationCenterService.markRead(principal, notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<Long> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Notifications marked as read", userNotificationCenterService.markAllRead(principal));
    }
}
