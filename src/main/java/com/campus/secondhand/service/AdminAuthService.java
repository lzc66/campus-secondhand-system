package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.AdminLoginRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.AdminLoginResponse;
import com.campus.secondhand.vo.admin.AdminProfileResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request, String ipAddress, String userAgent);

    AdminProfileResponse getCurrentAdmin(AdminPrincipal principal);
}
