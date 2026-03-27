package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.user.SaveItemRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserItemService;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.UserItemDetailResponse;
import com.campus.secondhand.vo.user.UserItemPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/user/items")
public class UserItemController {

    private final UserItemService userItemService;

    public UserItemController(UserItemService userItemService) {
        this.userItemService = userItemService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaFileResponse> uploadImage(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userItemService.uploadItemImage(principal, file));
    }

    @PostMapping
    public ApiResponse<UserItemDetailResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody SaveItemRequest request) {
        return ApiResponse.success(userItemService.createItem(principal, request));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<UserItemDetailResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long itemId,
                                                      @Valid @RequestBody SaveItemRequest request) {
        return ApiResponse.success(userItemService.updateItem(principal, itemId, request));
    }

    @GetMapping
    public ApiResponse<UserItemPageResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                  @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(userItemService.listMyItems(principal, status, page, size));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<UserItemDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long itemId) {
        return ApiResponse.success(userItemService.getMyItem(principal, itemId));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long itemId) {
        userItemService.deleteItem(principal, itemId);
        return ApiResponse.success("Item deleted", null);
    }
}