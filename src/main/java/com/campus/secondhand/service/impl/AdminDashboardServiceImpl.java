package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.TradeOrder;
import com.campus.secondhand.entity.User;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.AnnouncementPublishStatus;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.RegistrationStatus;
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
import com.campus.secondhand.service.AdminDashboardService;
import com.campus.secondhand.vo.admin.AdminDashboardCategorySalesResponse;
import com.campus.secondhand.vo.admin.AdminDashboardHotKeywordResponse;
import com.campus.secondhand.vo.admin.AdminDashboardItemStatusResponse;
import com.campus.secondhand.vo.admin.AdminDashboardOverviewResponse;
import com.campus.secondhand.vo.admin.AdminDashboardRecentActivityResponse;
import com.campus.secondhand.vo.admin.AdminDashboardTrendPointResponse;
import com.campus.secondhand.vo.admin.AdminDashboardUserGrowthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final RegistrationApplicationMapper registrationApplicationMapper;
    private final ItemMapper itemMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final AnnouncementMapper announcementMapper;
    private final WantedPostMapper wantedPostMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AdminMapper adminMapper;
    private final OrderItemMapper orderItemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final SearchHistoryMapper searchHistoryMapper;

    public AdminDashboardServiceImpl(UserMapper userMapper,
                                     RegistrationApplicationMapper registrationApplicationMapper,
                                     ItemMapper itemMapper,
                                     TradeOrderMapper tradeOrderMapper,
                                     AnnouncementMapper announcementMapper,
                                     WantedPostMapper wantedPostMapper,
                                     AdminOperationLogMapper adminOperationLogMapper,
                                     AdminMapper adminMapper,
                                     OrderItemMapper orderItemMapper,
                                     ItemCategoryMapper itemCategoryMapper,
                                     SearchHistoryMapper searchHistoryMapper) {
        this.userMapper = userMapper;
        this.registrationApplicationMapper = registrationApplicationMapper;
        this.itemMapper = itemMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.announcementMapper = announcementMapper;
        this.wantedPostMapper = wantedPostMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.adminMapper = adminMapper;
        this.orderItemMapper = orderItemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.searchHistoryMapper = searchHistoryMapper;
    }

    @Override
    public AdminDashboardOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();

        long totalUsers = count(userMapper.selectCount(new LambdaQueryWrapper<User>().isNull(User::getDeletedAt)));
        long activeUsers = count(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .isNull(User::getDeletedAt)
                .eq(User::getAccountStatus, com.campus.secondhand.enums.UserAccountStatus.ACTIVE)));
        long pendingRegistrationCount = count(registrationApplicationMapper.selectCount(new LambdaQueryWrapper<RegistrationApplication>()
                .eq(RegistrationApplication::getStatus, RegistrationStatus.PENDING)));
        long totalItems = count(itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .ne(Item::getStatus, ItemStatus.DELETED)));
        long onSaleItemCount = count(itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, ItemStatus.ON_SALE)));
        long totalOrders = count(tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()));
        long completedOrderCount = count(tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .isNotNull(TradeOrder::getCompletedAt)));
        long totalWantedPosts = count(wantedPostMapper.selectCount(new LambdaQueryWrapper<WantedPost>()
                .isNull(WantedPost::getDeletedAt)));
        long publishedAnnouncementCount = count(announcementMapper.selectCount(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getPublishStatus, AnnouncementPublishStatus.PUBLISHED)));
        long todayNewUsers = count(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .isNull(User::getDeletedAt)
                .ge(User::getCreatedAt, startOfToday)));
        long todayNewItems = count(itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .ne(Item::getStatus, ItemStatus.DELETED)
                .ge(Item::getCreatedAt, startOfToday)));
        long todayNewOrders = count(tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .ge(TradeOrder::getCreatedAt, startOfToday)));
        // SUM 聚合下推到 SQL,避免取回今日全部订单后在内存累加
        BigDecimal todayCompletedAmount = tradeOrderMapper.selectCompletedAmountSince(startOfToday);
        if (todayCompletedAmount == null) {
            todayCompletedAmount = BigDecimal.ZERO;
        }

        return new AdminDashboardOverviewResponse(
                totalUsers,
                activeUsers,
                pendingRegistrationCount,
                totalItems,
                onSaleItemCount,
                totalOrders,
                completedOrderCount,
                totalWantedPosts,
                publishedAnnouncementCount,
                todayNewUsers,
                todayNewItems,
                todayNewOrders,
                todayCompletedAmount
        );
    }

    @Override
    public List<AdminDashboardTrendPointResponse> getOrderTrends(int days) {
        DateRange range = resolvePastDaysRange(days, 30);
        return getOrderTrends(range.startDate(), range.endDate());
    }

    @Override
    public List<AdminDashboardTrendPointResponse> getOrderTrends(LocalDate startDate, LocalDate endDate) {
        DateRange range = validateRange(startDate, endDate, 365);
        Map<LocalDate, TrendAccumulator> trendMap = initTrendMap(range.startDate(), range.endDate());
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endExclusive = range.endDate().plusDays(1).atStartOfDay();

        // GROUP BY DATE(...) 聚合下推,不再取回区间内全部订单在内存统计
        applyTrendRows(trendMap, tradeOrderMapper.selectCreatedTrend(startDateTime, endExclusive), "created");
        applyTrendRows(trendMap, tradeOrderMapper.selectCompletedTrend(startDateTime, endExclusive), "completed");
        applyTrendRows(trendMap, tradeOrderMapper.selectCancelledTrend(startDateTime, endExclusive), "cancelled");
        return trendMap.entrySet().stream()
                .map(entry -> new AdminDashboardTrendPointResponse(
                        entry.getKey(),
                        entry.getValue().createdOrderCount,
                        entry.getValue().completedOrderCount,
                        entry.getValue().cancelledOrderCount,
                        entry.getValue().completedAmount
                ))
                .toList();
    }

    @Override
    public List<AdminDashboardItemStatusResponse> getItemStatusDistribution() {
        // 一次 GROUP BY 替代 6 次 count 查询
        Map<String, Long> countMap = itemMapper.selectStatusCounts().stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("status")),
                        row -> toLong(row.get("c")),
                        (a, b) -> a));
        return List.of(
                ItemStatus.DRAFT,
                ItemStatus.ON_SALE,
                ItemStatus.RESERVED,
                ItemStatus.SOLD,
                ItemStatus.OFF_SHELF,
                ItemStatus.DELETED
        ).stream().map(status -> new AdminDashboardItemStatusResponse(
                status.getValue(),
                countMap.getOrDefault(status.getValue(), 0L)
        )).toList();
    }

    @Override
    public List<AdminDashboardRecentActivityResponse> getRecentActivities(int limit) {
        Page<AdminOperationLog> page = new Page<>(1, normalizeLimit(limit));
        Page<AdminOperationLog> result = adminOperationLogMapper.selectPage(page, new LambdaQueryWrapper<AdminOperationLog>()
                .orderByDesc(AdminOperationLog::getCreatedAt)
                .orderByDesc(AdminOperationLog::getAdminOperationLogId));
        Map<Long, Admin> adminMap = loadAdmins(result.getRecords().stream().map(AdminOperationLog::getAdminId).distinct().toList());
        return result.getRecords().stream().map(log -> {
            Admin admin = adminMap.get(log.getAdminId());
            return new AdminDashboardRecentActivityResponse(
                    log.getAdminOperationLogId(),
                    log.getAdminId(),
                    admin == null ? null : admin.getAdminNo(),
                    admin == null ? null : admin.getAdminName(),
                    log.getTargetType(),
                    log.getTargetId(),
                    log.getOperationType(),
                    log.getOperationDetail(),
                    log.getCreatedAt()
            );
        }).toList();
    }

    @Override
    public List<AdminDashboardCategorySalesResponse> getCategorySalesRanking(int days, int limit) {
        DateRange range = resolvePastDaysRange(days, 365);
        return getCategorySalesRanking(range.startDate(), range.endDate(), limit);
    }

    @Override
    public List<AdminDashboardCategorySalesResponse> getCategorySalesRanking(LocalDate startDate, LocalDate endDate, int limit) {
        DateRange range = validateRange(startDate, endDate, 365);
        int normalizedLimit = normalizeLimit(limit);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endExclusive = range.endDate().plusDays(1).atStartOfDay();
        // JOIN + GROUP BY 聚合下推:不再取回区间全部订单/订单项、拼巨型 IN 后内存累加
        List<Map<String, Object>> rows = orderItemMapper.selectCategorySalesRanking(startDateTime, endExclusive, normalizedLimit);
        return rows.stream()
                .map(row -> new AdminDashboardCategorySalesResponse(
                        toNullableLong(row.get("categoryId")),
                        row.get("categoryName") == null ? null : String.valueOf(row.get("categoryName")),
                        toLong(row.get("soldQuantity")),
                        toLong(row.get("completedOrderCount")),
                        toBigDecimal(row.get("completedAmount"))
                ))
                .toList();
    }

    @Override
    public List<AdminDashboardHotKeywordResponse> getHotSearchKeywords(int days, int limit) {
        DateRange range = resolvePastDaysRange(days, 90);
        return getHotSearchKeywords(range.startDate(), range.endDate(), limit);
    }

    @Override
    public List<AdminDashboardHotKeywordResponse> getHotSearchKeywords(LocalDate startDate, LocalDate endDate, int limit) {
        DateRange range = validateRange(startDate, endDate, 365);
        int normalizedLimit = normalizeLimit(limit);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endExclusive = range.endDate().plusDays(1).atStartOfDay();
        // 只取回 (关键词, 分类) 去重后的计数行,而非区间内全部搜索记录
        List<Map<String, Object>> rows = searchHistoryMapper.selectKeywordCategoryCounts(startDateTime, endExclusive);
        Map<String, HotKeywordAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String keyword = normalizeKeyword(String.valueOf(row.get("keyword")));
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            long count = toLong(row.get("cnt"));
            HotKeywordAccumulator accumulator = accumulatorMap.computeIfAbsent(keyword, key -> new HotKeywordAccumulator());
            accumulator.searchCount += count;
            Object categoryIdObj = row.get("categoryId");
            if (categoryIdObj != null) {
                accumulator.categoryCountMap.merge(toLong(categoryIdObj), count, Long::sum);
            }
        }
        Set<Long> categoryIds = accumulatorMap.values().stream()
                .flatMap(accumulator -> accumulator.categoryCountMap.keySet().stream())
                .collect(Collectors.toSet());
        Map<Long, ItemCategory> categoryMap = loadCategories(categoryIds.stream().toList());
        return accumulatorMap.entrySet().stream()
                .map(entry -> {
                    Long topCategoryId = entry.getValue().categoryCountMap.entrySet().stream()
                            .max(Map.Entry.<Long, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    ItemCategory category = topCategoryId == null ? null : categoryMap.get(topCategoryId);
                    return new AdminDashboardHotKeywordResponse(
                            entry.getKey(),
                            entry.getValue().searchCount,
                            topCategoryId,
                            category == null ? null : category.getCategoryName()
                    );
                })
                .sorted(Comparator.comparing(AdminDashboardHotKeywordResponse::searchCount).reversed()
                        .thenComparing(AdminDashboardHotKeywordResponse::keyword))
                .limit(normalizedLimit)
                .toList();
    }

    @Override
    public List<AdminDashboardUserGrowthResponse> getUserGrowthTrends(int days) {
        DateRange range = resolvePastDaysRange(days, 90);
        return getUserGrowthTrends(range.startDate(), range.endDate());
    }

    @Override
    public List<AdminDashboardUserGrowthResponse> getUserGrowthTrends(LocalDate startDate, LocalDate endDate) {
        DateRange range = validateRange(startDate, endDate, 365);
        LocalDateTime startDateTime = range.startDate().atStartOfDay();
        LocalDateTime endExclusive = range.endDate().plusDays(1).atStartOfDay();
        long baseUserCount = count(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .isNull(User::getDeletedAt)
                .lt(User::getCreatedAt, startDateTime)));
        // GROUP BY DATE(...) 聚合下推,不再取回区间内全部用户
        Map<LocalDate, Long> dailyMap = userMapper.selectUserGrowth(startDateTime, endExclusive).stream()
                .filter(row -> row.get("d") instanceof java.sql.Date)
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row.get("d")).toLocalDate(),
                        row -> toLong(row.get("c")),
                        (a, b) -> a,
                        LinkedHashMap::new));
        List<AdminDashboardUserGrowthResponse> responses = new java.util.ArrayList<>();
        long cumulative = baseUserCount;
        for (LocalDate date = range.startDate(); !date.isAfter(range.endDate()); date = date.plusDays(1)) {
            long newUserCount = dailyMap.getOrDefault(date, 0L);
            cumulative += newUserCount;
            responses.add(new AdminDashboardUserGrowthResponse(date, newUserCount, cumulative));
        }
        return responses;
    }

    private Map<LocalDate, TrendAccumulator> initTrendMap(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, TrendAccumulator> trendMap = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            trendMap.put(date, new TrendAccumulator());
        }
        return trendMap;
    }

    private void applyTrendRows(Map<LocalDate, TrendAccumulator> trendMap, List<Map<String, Object>> rows, String kind) {
        for (Map<String, Object> row : rows) {
            if (!(row.get("d") instanceof java.sql.Date sqlDate)) {
                continue;
            }
            TrendAccumulator accumulator = trendMap.get(sqlDate.toLocalDate());
            if (accumulator == null) {
                continue;
            }
            long count = toLong(row.get("c"));
            switch (kind) {
                case "created" -> accumulator.createdOrderCount += count;
                case "completed" -> {
                    accumulator.completedOrderCount += count;
                    accumulator.completedAmount = accumulator.completedAmount.add(toBigDecimal(row.get("a")));
                }
                case "cancelled" -> accumulator.cancelledOrderCount += count;
                default -> {
                }
            }
        }
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Long toNullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return value instanceof Number number ? BigDecimal.valueOf(number.longValue()) : BigDecimal.ZERO;
    }

    private Map<Long, Admin> loadAdmins(List<Long> adminIds) {
        if (adminIds == null || adminIds.isEmpty()) {
            return Map.of();
        }
        return adminMapper.selectBatchIds(adminIds.stream().filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Admin::getAdminId, admin -> admin, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, ItemCategory> loadCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return itemCategoryMapper.selectBatchIds(categoryIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(ItemCategory::getCategoryId, category -> category, (a, b) -> a, LinkedHashMap::new));
    }

    private DateRange resolvePastDaysRange(int days, int maxDays) {
        int normalizedDays = normalizeDays(days, maxDays);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedDays - 1L);
        return new DateRange(startDate, endDate);
    }

    private DateRange validateRange(LocalDate startDate, LocalDate endDate, int maxDays) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be greater than or equal to startDate");
        }
        if (startDate.plusDays(maxDays - 1L).isBefore(endDate)) {
            throw new IllegalArgumentException("date range is too large");
        }
        return new DateRange(startDate, endDate);
    }

    private int normalizeDays(int days, int maxDays) {
        return Math.max(1, Math.min(days, maxDays));
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim().toLowerCase();
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class TrendAccumulator {
        private long createdOrderCount;
        private long completedOrderCount;
        private long cancelledOrderCount;
        private BigDecimal completedAmount = BigDecimal.ZERO;
    }

    private static final class HotKeywordAccumulator {
        private long searchCount;
        private Map<Long, Long> categoryCountMap = new LinkedHashMap<>();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}