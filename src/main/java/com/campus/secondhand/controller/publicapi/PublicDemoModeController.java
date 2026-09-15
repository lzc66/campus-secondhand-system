package com.campus.secondhand.controller.publicapi;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.AdminDemoModeService;
import com.campus.secondhand.vo.admin.DemoModeStatusResponse;
import com.campus.secondhand.vo.publicapi.PublicDemoModeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/demo-mode")
public class PublicDemoModeController {

    private final AdminDemoModeService adminDemoModeService;

    public PublicDemoModeController(AdminDemoModeService adminDemoModeService) {
        this.adminDemoModeService = adminDemoModeService;
    }

    /**
     * 公共端只暴露演示开关布尔值;演示数据统计(demoSummary)等经营数据仅管理端接口可见。
     */
    @GetMapping
    public ApiResponse<PublicDemoModeResponse> status() {
        DemoModeStatusResponse status = adminDemoModeService.getStatus();
        return ApiResponse.success(new PublicDemoModeResponse(
                status.demoModeEnabled(),
                status.demoItemNotesEnabled()
        ));
    }
}