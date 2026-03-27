package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.user.ChangePasswordRequest;
import com.campus.secondhand.dto.user.UpdateUserProfileRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserProfileService;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userProfileService.getProfile(principal));
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userProfileService.updateProfile(principal, request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(principal, request);
        return ApiResponse.success("Password changed", null);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaFileResponse> uploadAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userProfileService.uploadAvatar(principal, file));
    }
}