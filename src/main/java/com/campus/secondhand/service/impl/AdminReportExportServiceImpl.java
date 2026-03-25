package com.campus.secondhand.service.impl;

import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.service.AdminDashboardService;
import com.campus.secondhand.service.AdminReportExportService;
import com.campus.secondhand.vo.admin.AdminDashboardCategorySalesResponse;
import com.campus.secondhand.vo.admin.AdminDashboardHotKeywordResponse;
import com.campus.secondhand.vo.admin.AdminDashboardOverviewResponse;
import com.campus.secondhand.vo.admin.AdminDashboardTrendPointResponse;
import com.campus.secondhand.vo.admin.AdminDashboardUserGrowthResponse;
import com.campus.secondhand.vo.admin.AdminReportFileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminReportExportServiceImpl implements AdminReportExportService {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AdminDashboardService adminDashboardService;

    public AdminReportExportServiceImpl(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Override
    public AdminReportFileResponse exportDashboardOverviewReport() {
        AdminDashboardOverviewResponse overview = adminDashboardService.getOverview();
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("metric", "value"));
        rows.add(List.of("totalUsers", String.valueOf(overview.totalUsers())));
        rows.add(List.of("activeUsers", String.valueOf(overview.activeUsers())));
        rows.add(List.of("pendingRegistrationCount", String.valueOf(overview.pendingRegistrationCount())));
        rows.add(List.of("totalItems", String.valueOf(overview.totalItems())));
        rows.add(List.of("onSaleItemCount", String.valueOf(overview.onSaleItemCount())));
        rows.add(List.of("totalOrders", String.valueOf(overview.totalOrders())));
        rows.add(List.of("completedOrderCount", String.valueOf(overview.completedOrderCount())));
        rows.add(List.of("totalWantedPosts", String.valueOf(overview.totalWantedPosts())));
        rows.add(List.of("publishedAnnouncementCount", String.valueOf(overview.publishedAnnouncementCount())));
        rows.add(List.of("todayNewUsers", String.valueOf(overview.todayNewUsers())));
        rows.add(List.of("todayNewItems", String.valueOf(overview.todayNewItems())));
        rows.add(List.of("todayNewOrders", String.valueOf(overview.todayNewOrders())));
        rows.add(List.of("todayCompletedAmount", stringifyDecimal(overview.todayCompletedAmount())));
        return new AdminReportFileResponse(buildFileName("dashboard-overview"), CSV_CONTENT_TYPE, toCsvBytes(rows));
    }

    @Override
    public AdminReportFileResponse exportOrderTrendReport(int days) {
        return buildOrderTrendReport(adminDashboardService.getOrderTrends(days), "order-trends");
    }

    @Override
    public AdminReportFileResponse exportOrderTrendReport(LocalDate startDate, LocalDate endDate) {
        validateCustomRange(startDate, endDate);
        return buildOrderTrendReport(adminDashboardService.getOrderTrends(startDate, endDate), buildRangeFilePrefix("order-trends", startDate, endDate));
    }

    @Override
    public AdminReportFileResponse exportCategorySalesRankingReport(int days, int limit) {
        return buildCategorySalesRankingReport(adminDashboardService.getCategorySalesRanking(days, limit), "category-sales-ranking");
    }

    @Override
    public AdminReportFileResponse exportCategorySalesRankingReport(LocalDate startDate, LocalDate endDate, int limit) {
        validateCustomRange(startDate, endDate);
        return buildCategorySalesRankingReport(
                adminDashboardService.getCategorySalesRanking(startDate, endDate, limit),
                buildRangeFilePrefix("category-sales-ranking", startDate, endDate)
        );
    }

    @Override
    public AdminReportFileResponse exportHotSearchKeywordReport(int days, int limit) {
        return buildHotSearchKeywordReport(adminDashboardService.getHotSearchKeywords(days, limit), "hot-search-keywords");
    }

    @Override
    public AdminReportFileResponse exportHotSearchKeywordReport(LocalDate startDate, LocalDate endDate, int limit) {
        validateCustomRange(startDate, endDate);
        return buildHotSearchKeywordReport(
                adminDashboardService.getHotSearchKeywords(startDate, endDate, limit),
                buildRangeFilePrefix("hot-search-keywords", startDate, endDate)
        );
    }

    @Override
    public AdminReportFileResponse exportUserGrowthTrendReport(int days) {
        return buildUserGrowthTrendReport(adminDashboardService.getUserGrowthTrends(days), "user-growth-trends");
    }

    @Override
    public AdminReportFileResponse exportUserGrowthTrendReport(LocalDate startDate, LocalDate endDate) {
        validateCustomRange(startDate, endDate);
        return buildUserGrowthTrendReport(
                adminDashboardService.getUserGrowthTrends(startDate, endDate),
                buildRangeFilePrefix("user-growth-trends", startDate, endDate)
        );
    }

    private AdminReportFileResponse buildOrderTrendReport(List<AdminDashboardTrendPointResponse> trends, String filePrefix) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("date", "createdOrderCount", "completedOrderCount", "cancelledOrderCount", "completedAmount"));
        trends.forEach(point -> rows.add(List.of(
                stringifyDate(point.date()),
                String.valueOf(point.createdOrderCount()),
                String.valueOf(point.completedOrderCount()),
                String.valueOf(point.cancelledOrderCount()),
                stringifyDecimal(point.completedAmount())
        )));
        return new AdminReportFileResponse(buildFileName(filePrefix), CSV_CONTENT_TYPE, toCsvBytes(rows));
    }

    private AdminReportFileResponse buildCategorySalesRankingReport(List<AdminDashboardCategorySalesResponse> ranking, String filePrefix) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("categoryId", "categoryName", "soldQuantity", "completedOrderCount", "completedAmount"));
        ranking.forEach(item -> rows.add(List.of(
                stringifyLong(item.categoryId()),
                nullToEmpty(item.categoryName()),
                String.valueOf(item.soldQuantity()),
                String.valueOf(item.completedOrderCount()),
                stringifyDecimal(item.completedAmount())
        )));
        return new AdminReportFileResponse(buildFileName(filePrefix), CSV_CONTENT_TYPE, toCsvBytes(rows));
    }

    private AdminReportFileResponse buildHotSearchKeywordReport(List<AdminDashboardHotKeywordResponse> keywords, String filePrefix) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("keyword", "searchCount", "categoryId", "categoryName"));
        keywords.forEach(item -> rows.add(List.of(
                nullToEmpty(item.keyword()),
                String.valueOf(item.searchCount()),
                stringifyLong(item.categoryId()),
                nullToEmpty(item.categoryName())
        )));
        return new AdminReportFileResponse(buildFileName(filePrefix), CSV_CONTENT_TYPE, toCsvBytes(rows));
    }

    private AdminReportFileResponse buildUserGrowthTrendReport(List<AdminDashboardUserGrowthResponse> trends, String filePrefix) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("date", "newUserCount", "cumulativeUserCount"));
        trends.forEach(item -> rows.add(List.of(
                stringifyDate(item.date()),
                String.valueOf(item.newUserCount()),
                String.valueOf(item.cumulativeUserCount())
        )));
        return new AdminReportFileResponse(buildFileName(filePrefix), CSV_CONTENT_TYPE, toCsvBytes(rows));
    }

    private void validateCustomRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(40092, HttpStatus.BAD_REQUEST, "startDate and endDate must be provided together");
        }
    }

    private byte[] toCsvBytes(List<List<String>> rows) {
        String csv = rows.stream()
                .map(this::toCsvLine)
                .collect(Collectors.joining("\r\n", "\uFEFF", "\r\n"));
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private String toCsvLine(List<String> columns) {
        return columns.stream().map(this::escapeCsv).collect(Collectors.joining(","));
    }

    private String escapeCsv(String value) {
        String normalized = nullToEmpty(value);
        boolean shouldQuote = normalized.contains(",") || normalized.contains("\"") || normalized.contains("\r") || normalized.contains("\n");
        String escaped = normalized.replace("\"", "\"\"");
        return shouldQuote ? "\"" + escaped + "\"" : escaped;
    }

    private String buildFileName(String prefix) {
        return prefix + "-" + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".csv";
    }

    private String buildRangeFilePrefix(String prefix, LocalDate startDate, LocalDate endDate) {
        return prefix + "-" + startDate + "-to-" + endDate;
    }

    private String stringifyDecimal(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String stringifyDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String stringifyLong(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}