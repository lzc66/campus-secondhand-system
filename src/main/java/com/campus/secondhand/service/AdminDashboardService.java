package com.campus.secondhand.service;

import com.campus.secondhand.vo.admin.AdminDashboardCategorySalesResponse;
import com.campus.secondhand.vo.admin.AdminDashboardHotKeywordResponse;
import com.campus.secondhand.vo.admin.AdminDashboardItemStatusResponse;
import com.campus.secondhand.vo.admin.AdminDashboardOverviewResponse;
import com.campus.secondhand.vo.admin.AdminDashboardRecentActivityResponse;
import com.campus.secondhand.vo.admin.AdminDashboardTrendPointResponse;
import com.campus.secondhand.vo.admin.AdminDashboardUserGrowthResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardService {

    AdminDashboardOverviewResponse getOverview();

    List<AdminDashboardTrendPointResponse> getOrderTrends(int days);

    List<AdminDashboardTrendPointResponse> getOrderTrends(LocalDate startDate, LocalDate endDate);

    List<AdminDashboardItemStatusResponse> getItemStatusDistribution();

    List<AdminDashboardRecentActivityResponse> getRecentActivities(int limit);

    List<AdminDashboardCategorySalesResponse> getCategorySalesRanking(int days, int limit);

    List<AdminDashboardCategorySalesResponse> getCategorySalesRanking(LocalDate startDate, LocalDate endDate, int limit);

    List<AdminDashboardHotKeywordResponse> getHotSearchKeywords(int days, int limit);

    List<AdminDashboardHotKeywordResponse> getHotSearchKeywords(LocalDate startDate, LocalDate endDate, int limit);

    List<AdminDashboardUserGrowthResponse> getUserGrowthTrends(int days);

    List<AdminDashboardUserGrowthResponse> getUserGrowthTrends(LocalDate startDate, LocalDate endDate);
}