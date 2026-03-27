package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.user.CreateItemCommentRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.ItemCommentService;
import com.campus.secondhand.vo.publicapi.PublicItemCommentResponse;
import com.campus.secondhand.vo.user.UserReceivedCommentPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/user")
public class UserItemCommentController {

    private final ItemCommentService itemCommentService;

    public UserItemCommentController(ItemCommentService itemCommentService) {
        this.itemCommentService = itemCommentService;
    }

    @PostMapping("/items/{itemId}/comments")
    public ApiResponse<PublicItemCommentResponse> createComment(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable Long itemId,
                                                                @Valid @RequestBody CreateItemCommentRequest request) {
        return ApiResponse.success(itemCommentService.createComment(principal, itemId, request));
    }

    @PostMapping("/comments/{commentId}/reply")
    public ApiResponse<PublicItemCommentResponse> replyComment(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable Long commentId,
                                                               @Valid @RequestBody CreateItemCommentRequest request) {
        return ApiResponse.success(itemCommentService.replyComment(principal, commentId, request));
    }

    @GetMapping("/comments/received")
    public ApiResponse<UserReceivedCommentPageResponse> receivedComments(@AuthenticationPrincipal UserPrincipal principal,
                                                                         @RequestParam(required = false) Long itemId,
                                                                         @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                                         @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(itemCommentService.listReceivedComments(principal, itemId, page, size));
    }
}