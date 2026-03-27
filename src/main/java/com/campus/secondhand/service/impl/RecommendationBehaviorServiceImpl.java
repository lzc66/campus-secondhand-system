package com.campus.secondhand.service.impl;

import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.UserBehaviorLog;
import com.campus.secondhand.enums.SearchSortType;
import com.campus.secondhand.enums.UserBehaviorType;
import com.campus.secondhand.mapper.SearchHistoryMapper;
import com.campus.secondhand.mapper.UserBehaviorLogMapper;
import com.campus.secondhand.service.RecommendationBehaviorService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class RecommendationBehaviorServiceImpl implements RecommendationBehaviorService {

    private final SearchHistoryMapper searchHistoryMapper;
    private final UserBehaviorLogMapper userBehaviorLogMapper;

    public RecommendationBehaviorServiceImpl(SearchHistoryMapper searchHistoryMapper,
                                             UserBehaviorLogMapper userBehaviorLogMapper) {
        this.searchHistoryMapper = searchHistoryMapper;
        this.userBehaviorLogMapper = userBehaviorLogMapper;
    }

    @Override
    public void recordSearch(Long userId,
                             String keyword,
                             Long categoryId,
                             BigDecimal minPrice,
                             BigDecimal maxPrice,
                             String sortBy,
                             String sourcePage) {
        if (userId == null) {
            return;
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        searchHistoryMapper.insert(SearchHistory.builder()
                .userId(userId)
                .keyword(normalizedKeyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortType(parseSortType(sortBy))
                .build());
        userBehaviorLogMapper.insert(UserBehaviorLog.builder()
                .userId(userId)
                .behaviorType(UserBehaviorType.SEARCH)
                .sourcePage(sourcePage)
                .searchKeyword(normalizedKeyword)
                .weight(new BigDecimal("1.50"))
                .build());
    }

    @Override
    public void recordItemView(Long userId, Long itemId, String sourcePage) {
        if (userId == null || itemId == null) {
            return;
        }
        userBehaviorLogMapper.insert(UserBehaviorLog.builder()
                .userId(userId)
                .itemId(itemId)
                .behaviorType(UserBehaviorType.VIEW)
                .sourcePage(sourcePage)
                .weight(new BigDecimal("1.00"))
                .build());
    }

    @Override
    public void recordWantedPostPublish(Long userId, Long wantedPostId, String title) {
        if (userId == null || wantedPostId == null) {
            return;
        }
        userBehaviorLogMapper.insert(UserBehaviorLog.builder()
                .userId(userId)
                .wantedPostId(wantedPostId)
                .behaviorType(UserBehaviorType.WANT_POST)
                .sourcePage("user_wanted_posts")
                .searchKeyword(StringUtils.hasText(title) ? title.trim() : null)
                .weight(new BigDecimal("3.00"))
                .build());
    }

    @Override
    public void recordPurchase(Long userId, Long itemId, Long orderId) {
        if (userId == null || itemId == null) {
            return;
        }
        userBehaviorLogMapper.insert(UserBehaviorLog.builder()
                .userId(userId)
                .itemId(itemId)
                .behaviorType(UserBehaviorType.PURCHASE)
                .sourcePage("user_orders")
                .extraJson(orderId == null ? null : ("{\"orderId\":" + orderId + "}"))
                .weight(new BigDecimal("5.00"))
                .build());
    }

    private SearchSortType parseSortType(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return SearchSortType.DEFAULT;
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "latest" -> SearchSortType.LATEST;
            case "price_asc" -> SearchSortType.PRICE_ASC;
            case "price_desc" -> SearchSortType.PRICE_DESC;
            default -> SearchSortType.DEFAULT;
        };
    }
}

