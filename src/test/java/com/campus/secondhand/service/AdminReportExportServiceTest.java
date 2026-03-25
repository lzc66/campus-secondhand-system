package com.campus.secondhand.service;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.service.impl.AdminReportExportServiceImpl;
import com.campus.secondhand.vo.admin.AdminDashboardCategorySalesResponse;
import com.campus.secondhand.vo.admin.AdminDashboardHotKeywordResponse;
import com.campus.secondhand.vo.admin.AdminDashboardOverviewResponse;
import com.campus.secondhand.vo.admin.AdminDashboardTrendPointResponse;
import com.campus.secondhand.vo.admin.AdminDashboardUserGrowthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportExportServiceTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminReportExportServiceImpl adminReportExportService;

    @Test
    void shouldExportOverviewCsv() {
        when(adminDashboardService.getOverview()).thenReturn(new AdminDashboardOverviewResponse(
                10, 8, 2, 20, 6, 15, 5, 3, 4, 1, 2, 3, new BigDecimal("88.80")
        ));

        var report = adminReportExportService.exportDashboardOverviewReport();
        String csv = new String(report.content(), StandardCharsets.UTF_8);

        assertTrue(report.fileName().startsWith("dashboard-overview-"));
        assertTrue(report.fileName().endsWith(".csv"));
        assertTrue(csv.startsWith("\uFEFFmetric,value"));
        assertTrue(csv.contains("totalUsers,10"));
        assertTrue(csv.contains("todayCompletedAmount,88.8"));
    }

    @Test
    void shouldExportCategoryAndKeywordCsvWithEscaping() {
        when(adminDashboardService.getCategorySalesRanking(30, 10)).thenReturn(List.of(
                new AdminDashboardCategorySalesResponse(1L, "数码设备", 3, 2, new BigDecimal("188.80"))
        ));
        when(adminDashboardService.getHotSearchKeywords(7, 10)).thenReturn(List.of(
                new AdminDashboardHotKeywordResponse("ipad, 11\"", 4, 1L, "数码设备")
        ));

        var categoryReport = adminReportExportService.exportCategorySalesRankingReport(30, 10);
        var keywordReport = adminReportExportService.exportHotSearchKeywordReport(7, 10);
        String categoryCsv = new String(categoryReport.content(), StandardCharsets.UTF_8);
        String keywordCsv = new String(keywordReport.content(), StandardCharsets.UTF_8);

        assertTrue(categoryCsv.contains("categoryId,categoryName,soldQuantity,completedOrderCount,completedAmount"));
        assertTrue(categoryCsv.contains("1,数码设备,3,2,188.8"));
        assertTrue(keywordCsv.contains("keyword,searchCount,categoryId,categoryName"));
        assertTrue(keywordCsv.contains("\"ipad, 11\"\"\""));
    }

    @Test
    void shouldExportTrendReports() {
        when(adminDashboardService.getOrderTrends(7)).thenReturn(List.of(
                new AdminDashboardTrendPointResponse(LocalDate.of(2026, 3, 25), 3, 2, 1, new BigDecimal("120.50"))
        ));
        when(adminDashboardService.getUserGrowthTrends(7)).thenReturn(List.of(
                new AdminDashboardUserGrowthResponse(LocalDate.of(2026, 3, 25), 2, 18)
        ));

        var orderReport = adminReportExportService.exportOrderTrendReport(7);
        var userReport = adminReportExportService.exportUserGrowthTrendReport(7);
        String orderCsv = new String(orderReport.content(), StandardCharsets.UTF_8);
        String userCsv = new String(userReport.content(), StandardCharsets.UTF_8);

        assertTrue(orderCsv.contains("date,createdOrderCount,completedOrderCount,cancelledOrderCount,completedAmount"));
        assertTrue(orderCsv.contains("2026-03-25,3,2,1,120.5"));
        assertTrue(userCsv.contains("date,newUserCount,cumulativeUserCount"));
        assertTrue(userCsv.contains("2026-03-25,2,18"));
    }

    @Test
    void shouldExportCustomRangeReports() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 15);
        when(adminDashboardService.getOrderTrends(startDate, endDate)).thenReturn(List.of(
                new AdminDashboardTrendPointResponse(LocalDate.of(2026, 3, 10), 2, 1, 0, new BigDecimal("56.60"))
        ));
        when(adminDashboardService.getUserGrowthTrends(startDate, endDate)).thenReturn(List.of(
                new AdminDashboardUserGrowthResponse(LocalDate.of(2026, 3, 10), 1, 21)
        ));

        var orderReport = adminReportExportService.exportOrderTrendReport(startDate, endDate);
        var userReport = adminReportExportService.exportUserGrowthTrendReport(startDate, endDate);

        assertTrue(orderReport.fileName().startsWith("order-trends-2026-03-01-to-2026-03-15-"));
        assertTrue(userReport.fileName().startsWith("user-growth-trends-2026-03-01-to-2026-03-15-"));
    }

    @Test
    void shouldRejectIncompleteCustomRange() {
        assertThrows(BusinessException.class, () -> adminReportExportService.exportOrderTrendReport(LocalDate.of(2026, 3, 1), null));
    }
}