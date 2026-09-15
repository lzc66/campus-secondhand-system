package com.campus.secondhand.service;

import com.campus.secondhand.dto.admin.UpdateDemoModeRequest;
import com.campus.secondhand.security.AdminPrincipal;
import com.campus.secondhand.vo.admin.DemoDataSeedResponse;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;

public interface AdminDemoModeService {

    DemoModeStatusResponse getStatus();

    DemoDataSeedResponse seedDemoData(AdminPrincipal principal);

    DemoModeStatusResponse updateSettings(AdminPrincipal principal, UpdateDemoModeRequest request);

    /**
     * 清理全部演示数据(演示用户、商品、订单、求购、公告、评论、通知、推荐、搜索/行为记录、
     * 演示注册样例、媒体文件与磁盘文件)并重置演示开关。
     */
    DemoModeStatusResponse clearDemoData(AdminPrincipal principal);

    /**
     * 演示模式是否开启:关闭时公开列表过滤演示商品/求购,演示账号禁止登录。
     */
    boolean isDemoModeEnabled();
}
