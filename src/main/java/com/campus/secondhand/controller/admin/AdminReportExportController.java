package com.campus.secondhand.controller.admin;

import com.campus.secondhand.service.AdminReportExportService;
import com.campus.secondhand.vo.admin.AdminReportFileResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportExportController {

    private final AdminReportExportService adminReportExportService;

    public AdminReportExportController(AdminReportExportService adminReportExportService) {
        this.adminReportExportService = adminReportExportService;
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/dashboard-overview.csv")
    public ResponseEntity<byte[]> exportDashboardOverviewReport() {
        return toResponse(adminReportExportService.exportDashboardOverviewReport());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/order-trends.csv")
    public ResponseEntity<byte[]> exportOrderTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 30, message = "days must be less than or equal to 30") int days) {
        return toResponse(hasCustomRange(startDate, endDate)
                ? adminReportExportService.exportOrderTrendReport(startDate, endDate)
                : adminReportExportService.exportOrderTrendReport(days));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/category-sales-ranking.csv")
    public ResponseEntity<byte[]> exportCategorySalesRankingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "30") @Min(value = 1, message = "days must be greater than 0") @Max(value = 365, message = "days must be less than or equal to 365") int days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be less than or equal to 50") int limit) {
        return toResponse(hasCustomRange(startDate, endDate)
                ? adminReportExportService.exportCategorySalesRankingReport(startDate, endDate, limit)
                : adminReportExportService.exportCategorySalesRankingReport(days, limit));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/hot-search-keywords.csv")
    public ResponseEntity<byte[]> exportHotSearchKeywordReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 90, message = "days must be less than or equal to 90") int days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be greater than 0") @Max(value = 50, message = "limit must be less than or equal to 50") int limit) {
        return toResponse(hasCustomRange(startDate, endDate)
                ? adminReportExportService.exportHotSearchKeywordReport(startDate, endDate, limit)
                : adminReportExportService.exportHotSearchKeywordReport(days, limit));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDITOR','OPERATOR')")
    @GetMapping("/user-growth-trends.csv")
    public ResponseEntity<byte[]> exportUserGrowthTrendReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days must be greater than 0") @Max(value = 90, message = "days must be less than or equal to 90") int days) {
        return toResponse(hasCustomRange(startDate, endDate)
                ? adminReportExportService.exportUserGrowthTrendReport(startDate, endDate)
                : adminReportExportService.exportUserGrowthTrendReport(days));
    }

    private boolean hasCustomRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null || endDate != null;
    }

    private ResponseEntity<byte[]> toResponse(AdminReportFileResponse reportFile) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(reportFile.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(reportFile.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(reportFile.content());
    }
}