package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.enums.AdminAccountStatus;
import com.campus.secondhand.enums.AdminRoleCode;
import com.campus.secondhand.mapper.AdminMapper;
import com.campus.secondhand.mapper.AdminOperationLogMapper;
import com.campus.secondhand.mapper.AnnouncementMapper;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.OrderItemMapper;
import com.campus.secondhand.mapper.RegistrationApplicationMapper;
import com.campus.secondhand.mapper.SearchHistoryMapper;
import com.campus.secondhand.mapper.TradeOrderMapper;
import com.campus.secondhand.mapper.UserMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RegistrationApplicationMapper registrationApplicationMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private AnnouncementMapper announcementMapper;
    @Mock
    private WantedPostMapper wantedPostMapper;
    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private SearchHistoryMapper searchHistoryMapper;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    void shouldBuildOverview() {
        when(userMapper.selectCount(any())).thenReturn(12L, 10L, 2L);
        when(registrationApplicationMapper.selectCount(any())).thenReturn(3L);
        when(itemMapper.selectCount(any())).thenReturn(20L, 8L, 4L);
        when(tradeOrderMapper.selectCount(any())).thenReturn(15L, 6L, 5L);
        when(wantedPostMapper.selectCount(any())).thenReturn(7L);
        when(announcementMapper.selectCount(any())).thenReturn(4L);
        when(tradeOrderMapper.selectCompletedAmountSince(any())).thenReturn(new BigDecimal("200.00"));

        var response = adminDashboardService.getOverview();

        assertEquals(12L, response.totalUsers());
        assertEquals(10L, response.activeUsers());
        assertEquals(3L, response.pendingRegistrationCount());
        assertEquals(20L, response.totalItems());
        assertEquals(8L, response.onSaleItemCount());
        assertEquals(15L, response.totalOrders());
        assertEquals(6L, response.completedOrderCount());
        assertEquals(7L, response.totalWantedPosts());
        assertEquals(4L, response.publishedAnnouncementCount());
        assertEquals(2L, response.todayNewUsers());
        assertEquals(4L, response.todayNewItems());
        assertEquals(5L, response.todayNewOrders());
        assertEquals(new BigDecimal("200.00"), response.todayCompletedAmount());
    }

    @Test
    void shouldBuildOrderTrendsAndRecentActivities() {
        LocalDate today = LocalDate.now();
        when(tradeOrderMapper.selectCreatedTrend(any(), any())).thenReturn(List.of(
                Map.of("d", java.sql.Date.valueOf(today.minusDays(2)), "c", 1L),
                Map.of("d", java.sql.Date.valueOf(today.minusDays(1)), "c", 1L),
                Map.of("d", java.sql.Date.valueOf(today), "c", 1L)
        ));
        when(tradeOrderMapper.selectCompletedTrend(any(), any())).thenReturn(List.of(
                Map.of("d", java.sql.Date.valueOf(today.minusDays(1)), "c", 1L, "a", new BigDecimal("100.00")),
                Map.of("d", java.sql.Date.valueOf(today), "c", 1L, "a", new BigDecimal("88.80"))
        ));
        when(tradeOrderMapper.selectCancelledTrend(any(), any())).thenReturn(List.of(
                Map.of("d", java.sql.Date.valueOf(today.minusDays(1)), "c", 1L)
        ));
        doAnswer(invocation -> {
            Page<AdminOperationLog> page = invocation.getArgument(0);
            page.setTotal(2);
            page.setRecords(List.of(
                    AdminOperationLog.builder().adminOperationLogId(1L).adminId(9001L).targetType("item").targetId(101L).operationType("update_status").operationDetail("{}").createdAt(LocalDateTime.now()).build(),
                    AdminOperationLog.builder().adminOperationLogId(2L).adminId(9002L).targetType("order").targetId(201L).operationType("cancel").operationDetail("{}").createdAt(LocalDateTime.now().minusMinutes(5)).build()
            ));
            return page;
        }).when(adminOperationLogMapper).selectPage(any(), any());
        when(adminMapper.selectBatchIds(any())).thenReturn(List.of(
                Admin.builder().adminId(9001L).adminNo("admin1001").adminName("Campus Admin").roleCode(AdminRoleCode.SUPER_ADMIN).accountStatus(AdminAccountStatus.ACTIVE).build(),
                Admin.builder().adminId(9002L).adminNo("admin1002").adminName("Operator").roleCode(AdminRoleCode.OPERATOR).accountStatus(AdminAccountStatus.ACTIVE).build()
        ));

        var trends = adminDashboardService.getOrderTrends(3);
        var activities = adminDashboardService.getRecentActivities(10);

        assertEquals(3, trends.size());
        assertEquals(1L, trends.get(0).createdOrderCount());
        assertEquals(1L, trends.get(1).completedOrderCount());
        assertEquals(1L, trends.get(1).cancelledOrderCount());
        assertEquals(new BigDecimal("88.80"), trends.get(2).completedAmount());
        assertEquals(2, activities.size());
        assertEquals("admin1001", activities.get(0).adminNo());
        assertNotNull(activities.get(1).createdAt());
    }

    @Test
    void shouldBuildCategorySalesHotKeywordsAndUserGrowth() {
        LocalDate today = LocalDate.now();
        when(orderItemMapper.selectCategorySalesRanking(any(), any(), anyInt())).thenReturn(List.of(
                Map.of("categoryId", 10L, "categoryName", "数码设备", "soldQuantity", 2L, "completedOrderCount", 1L, "completedAmount", new BigDecimal("100.00")),
                Map.of("categoryId", 20L, "categoryName", "教材书籍", "soldQuantity", 1L, "completedOrderCount", 1L, "completedAmount", new BigDecimal("88.80"))
        ));
        when(searchHistoryMapper.selectKeywordCategoryCounts(any(), any())).thenReturn(List.of(
                Map.of("keyword", "ipad", "categoryId", 10L, "cnt", 2L),
                Map.of("keyword", " iPad ", "categoryId", 10L, "cnt", 1L),
                Map.of("keyword", "java", "categoryId", 20L, "cnt", 1L)
        ));
        when(itemCategoryMapper.selectBatchIds(any())).thenReturn(List.of(
                ItemCategory.builder().categoryId(10L).categoryName("数码设备").build(),
                ItemCategory.builder().categoryId(20L).categoryName("教材书籍").build()
        ));
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(userMapper.selectUserGrowth(any(), any())).thenReturn(List.of(
                Map.of("d", java.sql.Date.valueOf(today.minusDays(2)), "c", 1L),
                Map.of("d", java.sql.Date.valueOf(today.minusDays(1)), "c", 2L),
                Map.of("d", java.sql.Date.valueOf(today), "c", 1L)
        ));

        var categoryRanking = adminDashboardService.getCategorySalesRanking(30, 10);
        var hotKeywords = adminDashboardService.getHotSearchKeywords(7, 10);
        var userGrowth = adminDashboardService.getUserGrowthTrends(3);

        assertEquals(2, categoryRanking.size());
        assertEquals("数码设备", categoryRanking.get(0).categoryName());
        assertEquals(2L, categoryRanking.get(0).soldQuantity());
        assertEquals(new BigDecimal("100.00"), categoryRanking.get(0).completedAmount());
        assertEquals(2, hotKeywords.size());
        assertEquals("ipad", hotKeywords.get(0).keyword());
        assertEquals(3L, hotKeywords.get(0).searchCount());
        assertEquals("数码设备", hotKeywords.get(0).categoryName());
        assertEquals(3, userGrowth.size());
        assertEquals(1L, userGrowth.get(0).newUserCount());
        assertEquals(6L, userGrowth.get(0).cumulativeUserCount());
        assertEquals(8L, userGrowth.get(1).cumulativeUserCount());
        assertEquals(9L, userGrowth.get(2).cumulativeUserCount());
    }
}
