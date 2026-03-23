package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.PublicItemService;
import com.campus.secondhand.vo.publicapi.PublicItemDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicItemPageResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequestMapping("/api/v1/public/items")
public class PublicItemController {

    private final PublicItemService publicItemService;

    public PublicItemController(PublicItemService publicItemService) {
        this.publicItemService = publicItemService;
    }

    @GetMapping
    public ApiResponse<PublicItemPageResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true, message = "priceMin must be greater than or equal to 0") BigDecimal priceMin,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true, message = "priceMax must be greater than or equal to 0") BigDecimal priceMax,
            @RequestParam(required = false) String conditionLevel,
            @RequestParam(required = false) String tradeMode,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(publicItemService.listItems(
                principal == null ? null : principal.getUserId(),
                categoryId,
                keyword,
                brand,
                priceMin,
                priceMax,
                conditionLevel,
                tradeMode,
                sortBy,
                page,
                size
        ));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<PublicItemDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long itemId) {
        return ApiResponse.success(publicItemService.getItemDetail(principal == null ? null : principal.getUserId(), itemId));
    }
}
