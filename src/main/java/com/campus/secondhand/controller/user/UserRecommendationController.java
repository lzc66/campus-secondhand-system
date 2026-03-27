package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserRecommendationService;
import com.campus.secondhand.vo.user.UserRecommendationPageResponse;
import jakarta.validation.constraints.Max;
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
@RequestMapping("/api/v1/user/recommendations")
public class UserRecommendationController {

    private final UserRecommendationService userRecommendationService;

    public UserRecommendationController(UserRecommendationService userRecommendationService) {
        this.userRecommendationService = userRecommendationService;
    }

    @GetMapping
    public ApiResponse<UserRecommendationPageResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size,
                                                            @RequestParam(defaultValue = "false") boolean refresh) {
        return ApiResponse.success(userRecommendationService.listRecommendations(principal, page, size, refresh));
    }

    @PostMapping("/refresh")
    public ApiResponse<UserRecommendationPageResponse> refresh(@AuthenticationPrincipal UserPrincipal principal,
                                                               @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be at most 50") int limit) {
        return ApiResponse.success(userRecommendationService.refreshRecommendations(principal, limit));
    }

    @PostMapping("/{recommendationId}/click")
    public ApiResponse<Void> click(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long recommendationId) {
        userRecommendationService.markClicked(principal, recommendationId);
        return ApiResponse.success("Recommendation click recorded", null);
    }
}
