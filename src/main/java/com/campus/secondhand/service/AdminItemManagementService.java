package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.UpdateItemStatusRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.AdminItemDetailResponse;
import com.campus.secondhand.vo.admin.AdminItemPageResponse;

public interface AdminItemManagementService {

    AdminItemPageResponse list(String status, Long categoryId, String keyword, String sellerStudentNo, long page, long size);

    AdminItemDetailResponse detail(Long itemId);

    AdminItemDetailResponse updateStatus(AdminPrincipal principal, Long itemId, UpdateItemStatusRequest request);
}
