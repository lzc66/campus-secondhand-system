package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.AdminOrderActionRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.AdminOrderDetailResponse;
import com.campus.secondhand.vo.admin.AdminOrderPageResponse;

public interface AdminOrderManagementService {

    AdminOrderPageResponse list(String orderStatus, String orderNo, String buyerStudentNo, String sellerStudentNo, long page, long size);

    AdminOrderDetailResponse detail(Long orderId);

    AdminOrderDetailResponse cancel(AdminPrincipal principal, Long orderId, AdminOrderActionRequest request);

    AdminOrderDetailResponse close(AdminPrincipal principal, Long orderId, AdminOrderActionRequest request);
}
