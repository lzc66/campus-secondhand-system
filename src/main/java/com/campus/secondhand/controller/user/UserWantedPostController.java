package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.user.SaveWantedPostRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserWantedPostService;
import com.campus.secondhand.vo.user.UserWantedPostDetailResponse;
import com.campus.secondhand.vo.user.UserWantedPostPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/user/wanted-posts")
public class UserWantedPostController {

    private final UserWantedPostService userWantedPostService;

    public UserWantedPostController(UserWantedPostService userWantedPostService) {
        this.userWantedPostService = userWantedPostService;
    }

    @PostMapping
    public ApiResponse<UserWantedPostDetailResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                            @Valid @RequestBody SaveWantedPostRequest request) {
        return ApiResponse.success(userWantedPostService.createWantedPost(principal, request));
    }

    @PutMapping("/{wantedPostId}")
    public ApiResponse<UserWantedPostDetailResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable Long wantedPostId,
                                                            @Valid @RequestBody SaveWantedPostRequest request) {
        return ApiResponse.success(userWantedPostService.updateWantedPost(principal, wantedPostId, request));
    }

    @GetMapping
    public ApiResponse<UserWantedPostPageResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                        @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(userWantedPostService.listMyWantedPosts(principal, status, page, size));
    }

    @GetMapping("/{wantedPostId}")
    public ApiResponse<UserWantedPostDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable Long wantedPostId) {
        return ApiResponse.success(userWantedPostService.getMyWantedPost(principal, wantedPostId));
    }

    @DeleteMapping("/{wantedPostId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long wantedPostId) {
        userWantedPostService.deleteWantedPost(principal, wantedPostId);
        return ApiResponse.success("Wanted post deleted", null);
    }
}
