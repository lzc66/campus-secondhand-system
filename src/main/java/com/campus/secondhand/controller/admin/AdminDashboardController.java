package com.campus.secondhand.controller.admin;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.service.AdminDashboardService;
import com.campus.secondhand.vo.admin.AdminDashboardCategorySalesResponse;
import com.campus.secondhand.vo.admin.AdminDashboardHotKeywordResponse;
import com.campus.secondhand.vo.admin.AdminDashboardItemStatusResponse;
import com.campus.secondhand.vo.admin.AdminDashboardOverviewResponse;
import com.campus.secondhand.vo.admin.AdminDashboardRecentActivityResponse;
import com.campus.secondhand.vo.admin.AdminDashboardTrendPointResponse;
import com.campus.secondhand.vo.admin.AdminDashboardUserGrowthResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/overview")
    public ApiResponse<AdminDashboardOverviewResponse> overview() {
        return ApiResponse.success(adminDashboardService.getOverview());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/order-trends")
    public ApiResponse<List<AdminDashboardTrendPointResponse>> orderTrends(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 30, message = "days must be less than or equal to 30") int days) {
        return ApiResponse.success(adminDashboardService.getOrderTrends(days));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/item-status")
    public ApiResponse<List<AdminDashboardItemStatusResponse>> itemStatusDistribution() {
        return ApiResponse.success(adminDashboardService.getItemStatusDistribution());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/recent-activities")
    public ApiResponse<List<AdminDashboardRecentActivityResponse>> recentActivities(
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be less than or equal to 50") int limit) {
        return ApiResponse.success(adminDashboardService.getRecentActivities(limit));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/category-sales-ranking")
    public ApiResponse<List<AdminDashboardCategorySalesResponse>> categorySalesRanking(
            @RequestParam(defaultValue = "30") @Min(value = 1, message = "days must be greater than 0") @Max(value = 365, message = "days must be less than or equal to 365") int days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be less than or equal to 50") int limit) {
        return ApiResponse.success(adminDashboardService.getCategorySalesRanking(days, limit));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/hot-search-keywords")
    public ApiResponse<List<AdminDashboardHotKeywordResponse>> hotSearchKeywords(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 90, message = "days must be less than or equal to 90") int days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be less than or equal to 50") int limit) {
        return ApiResponse.success(adminDashboardService.getHotSearchKeywords(days, limit));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/user-growth-trends")
    public ApiResponse<List<AdminDashboardUserGrowthResponse>> userGrowthTrends(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 90, message = "days must be less than or equal to 90") int days) {
        return ApiResponse.success(adminDashboardService.getUserGrowthTrends(days));
    }
}