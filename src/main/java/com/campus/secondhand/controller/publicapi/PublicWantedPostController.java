package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.PublicWantedPostService;
import com.campus.secondhand.vo.publicapi.PublicWantedPostDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicWantedPostPageResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/public/wanted-posts")
public class PublicWantedPostController {

    private final PublicWantedPostService publicWantedPostService;

    public PublicWantedPostController(PublicWantedPostService publicWantedPostService) {
        this.publicWantedPostService = publicWantedPostService;
    }

    @GetMapping
    public ApiResponse<PublicWantedPostPageResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true, message = "priceMin must be greater than or equal to 0") BigDecimal priceMin,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true, message = "priceMax must be greater than or equal to 0") BigDecimal priceMax,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(publicWantedPostService.listWantedPosts(
                categoryId,
                keyword,
                brand,
                priceMin,
                priceMax,
                sortBy,
                page,
                size
        ));
    }

    @GetMapping("/{wantedPostId}")
    public ApiResponse<PublicWantedPostDetailResponse> detail(@PathVariable Long wantedPostId) {
        return ApiResponse.success(publicWantedPostService.getWantedPostDetail(wantedPostId));
    }
}
