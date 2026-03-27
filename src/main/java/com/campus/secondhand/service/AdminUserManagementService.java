package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.UpdateUserStatusRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.AdminUserDetailResponse;
import com.campus.secondhand.vo.admin.AdminUserPageResponse;

public interface AdminUserManagementService {

    AdminUserPageResponse list(String accountStatus, String studentNo, String realName, long page, long size);

    AdminUserDetailResponse detail(Long userId);

    AdminUserDetailResponse updateStatus(AdminPrincipal principal, Long userId, UpdateUserStatusRequest request);
}
