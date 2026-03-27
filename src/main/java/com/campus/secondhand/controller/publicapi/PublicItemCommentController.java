package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.ItemCommentService;
import com.campus.secondhand.vo.publicapi.PublicItemCommentPageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/items/{itemId}/comments")
public class PublicItemCommentController {

    private final ItemCommentService itemCommentService;

    public PublicItemCommentController(ItemCommentService itemCommentService) {
        this.itemCommentService = itemCommentService;
    }

    @GetMapping
    public ApiResponse<PublicItemCommentPageResponse> list(@PathVariable Long itemId,
                                                           @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                           @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(itemCommentService.listPublicComments(itemId, page, size));
    }
}