package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.TradeOrder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(
                TradeOrder.builder().totalAmount(new BigDecimal("120.50")).completedAt(LocalDateTime.now()).build(),
                TradeOrder.builder().totalAmount(new BigDecimal("79.50")).completedAt(LocalDateTime.now()).build()
        ));

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
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(
                TradeOrder.builder()
                        .createdAt(today.minusDays(2).atTime(10, 0))
                        .completedAt(today.minusDays(1).atTime(18, 0))
                        .totalAmount(new BigDecimal("100.00"))
                        .build(),
                TradeOrder.builder()
                        .createdAt(today.minusDays(1).atTime(11, 0))
                        .cancelledAt(today.minusDays(1).atTime(20, 0))
                        .totalAmount(new BigDecimal("50.00"))
                        .build(),
                TradeOrder.builder()
                        .createdAt(today.atTime(9, 30))
                        .completedAt(today.atTime(21, 0))
                        .totalAmount(new BigDecimal("88.80"))
                        .build()
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
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(
                TradeOrder.builder().orderId(1L).completedAt(today.minusDays(1).atTime(18, 0)).totalAmount(new BigDecimal("100.00")).build(),
                TradeOrder.builder().orderId(2L).completedAt(today.atTime(20, 0)).totalAmount(new BigDecimal("88.80")).build()
        ));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(
                OrderItem.builder().orderId(1L).itemId(101L).quantity(2).subtotalAmount(new BigDecimal("100.00")).build(),
                OrderItem.builder().orderId(2L).itemId(102L).quantity(1).subtotalAmount(new BigDecimal("88.80")).build()
        ));
        when(itemMapper.selectBatchIds(any())).thenReturn(List.of(
                Item.builder().itemId(101L).categoryId(10L).build(),
                Item.builder().itemId(102L).categoryId(20L).build()
        ));
        when(itemCategoryMapper.selectBatchIds(any())).thenReturn(List.of(
                ItemCategory.builder().categoryId(10L).categoryName("数码设备").build(),
                ItemCategory.builder().categoryId(20L).categoryName("教材书籍").build()
        ));
        when(searchHistoryMapper.selectList(any())).thenReturn(List.of(
                SearchHistory.builder().keyword("ipad").categoryId(10L).searchedAt(LocalDateTime.now()).build(),
                SearchHistory.builder().keyword("ipad").categoryId(10L).searchedAt(LocalDateTime.now()).build(),
                SearchHistory.builder().keyword("java").categoryId(20L).searchedAt(LocalDateTime.now()).build(),
                SearchHistory.builder().keyword(" iPad ").categoryId(10L).searchedAt(LocalDateTime.now()).build()
        ));
        when(userMapper.selectCount(any())).thenReturn(5L);
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().userId(1L).createdAt(today.minusDays(2).atTime(8, 0)).build(),
                User.builder().userId(2L).createdAt(today.minusDays(1).atTime(9, 0)).build(),
                User.builder().userId(3L).createdAt(today.minusDays(1).atTime(10, 0)).build(),
                User.builder().userId(4L).createdAt(today.atTime(11, 0)).build()
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