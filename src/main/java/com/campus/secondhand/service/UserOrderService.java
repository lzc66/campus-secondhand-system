package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.CancelOrderRequest;
import com.campus.secondhand.dto.user.CreateOrderRequest;
import com.campus.secondhand.dto.user.OrderActionRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.user.UserOrderDetailResponse;
import com.campus.secondhand.vo.user.UserOrderPageResponse;

public interface UserOrderService {

    UserOrderDetailResponse createOrder(UserPrincipal principal, CreateOrderRequest request);

    UserOrderPageResponse listOrders(UserPrincipal principal, String role, String status, long page, long size);

    UserOrderDetailResponse getOrderDetail(UserPrincipal principal, Long orderId);

    UserOrderDetailResponse confirmOrder(UserPrincipal principal, Long orderId, OrderActionRequest request);

    UserOrderDetailResponse markDelivering(UserPrincipal principal, Long orderId, OrderActionRequest request);

    UserOrderDetailResponse completeOrder(UserPrincipal principal, Long orderId, OrderActionRequest request);

    UserOrderDetailResponse cancelOrder(UserPrincipal principal, Long orderId, CancelOrderRequest request);
}
