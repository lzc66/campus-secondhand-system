package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Admin;
import com.campus.secondhand.entity.AdminOperationLog;
import com.campus.secondhand.entity.Announcement;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.OrderItem;
import com.campus.secondhand.entity.RegistrationApplication;
import com.campus.secondhand.entity.SearchHistory;
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
        BigDecimal todayCompletedAmount = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                        .isNotNull(TradeOrder::getCompletedAt)
                        .ge(TradeOrder::getCompletedAt, startOfToday))
                .stream()
                .map(TradeOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        List<TradeOrder> orders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .and(wrapper -> wrapper
                        .and(q -> q.ge(TradeOrder::getCreatedAt, startDateTime).lt(TradeOrder::getCreatedAt, endExclusive))
                        .or().and(q -> q.ge(TradeOrder::getCompletedAt, startDateTime).lt(TradeOrder::getCompletedAt, endExclusive))
                        .or().and(q -> q.ge(TradeOrder::getCancelledAt, startDateTime).lt(TradeOrder::getCancelledAt, endExclusive))));
        for (TradeOrder order : orders) {
            applyCreatedTrend(trendMap, order);
            applyCompletedTrend(trendMap, order);
            applyCancelledTrend(trendMap, order);
        }
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
        return List.of(
                ItemStatus.DRAFT,
                ItemStatus.ON_SALE,
                ItemStatus.RESERVED,
                ItemStatus.SOLD,
                ItemStatus.OFF_SHELF,
                ItemStatus.DELETED
        ).stream().map(status -> new AdminDashboardItemStatusResponse(
                status.getValue(),
                count(itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, status)))
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
        List<TradeOrder> completedOrders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .ge(TradeOrder::getCompletedAt, startDateTime)
                .lt(TradeOrder::getCompletedAt, endExclusive));
        if (completedOrders.isEmpty()) {
            return List.of();
        }
        Map<Long, TradeOrder> orderMap = completedOrders.stream().collect(Collectors.toMap(TradeOrder::getOrderId, order -> order));
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderMap.keySet()));
        if (orderItems.isEmpty()) {
            return List.of();
        }
        Map<Long, Item> itemMap = loadItems(orderItems.stream().map(OrderItem::getItemId).distinct().toList());
        Map<Long, ItemCategory> categoryMap = loadCategories(itemMap.values().stream().map(Item::getCategoryId).filter(Objects::nonNull).distinct().toList());
        Map<Long, CategorySalesAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (OrderItem orderItem : orderItems) {
            Item item = itemMap.get(orderItem.getItemId());
            if (item == null || item.getCategoryId() == null) {
                continue;
            }
            CategorySalesAccumulator accumulator = accumulatorMap.computeIfAbsent(item.getCategoryId(), key -> new CategorySalesAccumulator());
            accumulator.soldQuantity += orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            accumulator.completedAmount = accumulator.completedAmount.add(defaultAmount(orderItem.getSubtotalAmount()));
            accumulator.orderIds.add(orderItem.getOrderId());
        }
        return accumulatorMap.entrySet().stream()
                .map(entry -> new AdminDashboardCategorySalesResponse(
                        entry.getKey(),
                        categoryMap.containsKey(entry.getKey()) ? categoryMap.get(entry.getKey()).getCategoryName() : null,
                        entry.getValue().soldQuantity,
                        entry.getValue().orderIds.size(),
                        entry.getValue().completedAmount
                ))
                .sorted(Comparator.comparing(AdminDashboardCategorySalesResponse::completedAmount).reversed()
                        .thenComparing(AdminDashboardCategorySalesResponse::soldQuantity, Comparator.reverseOrder())
                        .thenComparing(response -> response.categoryId() == null ? Long.MAX_VALUE : response.categoryId()))
                .limit(normalizedLimit)
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
        List<SearchHistory> histories = searchHistoryMapper.selectList(new LambdaQueryWrapper<SearchHistory>()
                .ge(SearchHistory::getSearchedAt, startDateTime)
                .lt(SearchHistory::getSearchedAt, endExclusive));
        if (histories.isEmpty()) {
            return List.of();
        }
        Map<String, HotKeywordAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (SearchHistory history : histories) {
            String keyword = normalizeKeyword(history.getKeyword());
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            HotKeywordAccumulator accumulator = accumulatorMap.computeIfAbsent(keyword, key -> new HotKeywordAccumulator());
            accumulator.searchCount++;
            if (history.getCategoryId() != null) {
                accumulator.categoryCountMap.merge(history.getCategoryId(), 1L, Long::sum);
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
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .isNull(User::getDeletedAt)
                .ge(User::getCreatedAt, startDateTime)
                .lt(User::getCreatedAt, endExclusive)
                .orderByAsc(User::getCreatedAt));
        Map<LocalDate, Long> dailyMap = users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(user -> user.getCreatedAt().toLocalDate(), LinkedHashMap::new, Collectors.counting()));
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

    private void applyCreatedTrend(Map<LocalDate, TrendAccumulator> trendMap, TradeOrder order) {
        if (order.getCreatedAt() == null) {
            return;
        }
        TrendAccumulator accumulator = trendMap.get(order.getCreatedAt().toLocalDate());
        if (accumulator != null) {
            accumulator.createdOrderCount++;
        }
    }

    private void applyCompletedTrend(Map<LocalDate, TrendAccumulator> trendMap, TradeOrder order) {
        if (order.getCompletedAt() == null) {
            return;
        }
        TrendAccumulator accumulator = trendMap.get(order.getCompletedAt().toLocalDate());
        if (accumulator != null) {
            accumulator.completedOrderCount++;
            accumulator.completedAmount = accumulator.completedAmount.add(defaultAmount(order.getTotalAmount()));
        }
    }

    private void applyCancelledTrend(Map<LocalDate, TrendAccumulator> trendMap, TradeOrder order) {
        if (order.getCancelledAt() == null) {
            return;
        }
        TrendAccumulator accumulator = trendMap.get(order.getCancelledAt().toLocalDate());
        if (accumulator != null) {
            accumulator.cancelledOrderCount++;
        }
    }

    private Map<Long, Admin> loadAdmins(List<Long> adminIds) {
        if (adminIds == null || adminIds.isEmpty()) {
            return Map.of();
        }
        return adminMapper.selectBatchIds(adminIds.stream().filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Admin::getAdminId, admin -> admin, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, Item> loadItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        return itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item, (a, b) -> a, LinkedHashMap::new));
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

    private static final class CategorySalesAccumulator {
        private long soldQuantity;
        private BigDecimal completedAmount = BigDecimal.ZERO;
        private Set<Long> orderIds = new java.util.HashSet<>();
    }

    private static final class HotKeywordAccumulator {
        private long searchCount;
        private Map<Long, Long> categoryCountMap = new LinkedHashMap<>();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}