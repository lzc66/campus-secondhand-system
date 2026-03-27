package com.campus.secondhand.service;

import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.BootstrapResponse;
import com.campus.secondhand.vo.admin.InitStatusResponse;

public interface SystemInitService {

    BootstrapResponse bootstrap(AdminPrincipal principal);

    InitStatusResponse getStatus(AdminPrincipal principal);
}
