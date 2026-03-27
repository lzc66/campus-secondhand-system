package com.campus.secondhand.service;

import com.campus.secondhand.vo.admin.AdminReportFileResponse;

import java.time.LocalDate;

public interface AdminReportExportService {

    AdminReportFileResponse exportDashboardOverviewReport();

    AdminReportFileResponse exportOrderTrendReport(int days);

    AdminReportFileResponse exportOrderTrendReport(LocalDate startDate, LocalDate endDate);

    AdminReportFileResponse exportCategorySalesRankingReport(int days, int limit);

    AdminReportFileResponse exportCategorySalesRankingReport(LocalDate startDate, LocalDate endDate, int limit);

    AdminReportFileResponse exportHotSearchKeywordReport(int days, int limit);

    AdminReportFileResponse exportHotSearchKeywordReport(LocalDate startDate, LocalDate endDate, int limit);

    AdminReportFileResponse exportUserGrowthTrendReport(int days);

    AdminReportFileResponse exportUserGrowthTrendReport(LocalDate startDate, LocalDate endDate);
}