package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.UpdateDemoModeRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.DemoDataSeedResponse;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;

public interface AdminDemoModeService {

    DemoModeStatusResponse getStatus();

    DemoDataSeedResponse seedDemoData(AdminPrincipal principal);

    DemoModeStatusResponse updateSettings(AdminPrincipal principal, UpdateDemoModeRequest request);
}