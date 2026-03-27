package com.campus.secondhand.controller.user;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.dto.user.CancelOrderRequest;
import com.campus.secondhand.dto.user.CreateOrderRequest;
import com.campus.secondhand.dto.user.OrderActionRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.UserOrderService;
import com.campus.secondhand.vo.user.UserOrderDetailResponse;
import com.campus.secondhand.vo.user.UserOrderPageResponse;
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
@RequestMapping("/api/v1/user/orders")
public class UserOrderController {

    private final UserOrderService userOrderService;

    public UserOrderController(UserOrderService userOrderService) {
        this.userOrderService = userOrderService;
    }

    @PostMapping
    public ApiResponse<UserOrderDetailResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(userOrderService.createOrder(principal, request));
    }

    @GetMapping
    public ApiResponse<UserOrderPageResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") @Min(value = 1, message = "page must be greater than 0") long page,
                                                   @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be greater than 0") long size) {
        return ApiResponse.success(userOrderService.listOrders(principal, role, status, page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<UserOrderDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long orderId) {
        return ApiResponse.success(userOrderService.getOrderDetail(principal, orderId));
    }

    @PostMapping("/{orderId}/confirm")
    public ApiResponse<UserOrderDetailResponse> confirm(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long orderId,
                                                        @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(userOrderService.confirmOrder(principal, orderId, request));
    }

    @PostMapping("/{orderId}/deliver")
    public ApiResponse<UserOrderDetailResponse> deliver(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long orderId,
                                                        @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(userOrderService.markDelivering(principal, orderId, request));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<UserOrderDetailResponse> complete(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable Long orderId,
                                                         @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(userOrderService.completeOrder(principal, orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<UserOrderDetailResponse> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long orderId,
                                                       @Valid @RequestBody CancelOrderRequest request) {
        return ApiResponse.success(userOrderService.cancelOrder(principal, orderId, request));
    }
}
