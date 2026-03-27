package com.campus.secondhand.service;

import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.UserBehaviorLog;
import com.campus.secondhand.enums.SearchSortType;
import com.campus.secondhand.enums.UserBehaviorType;
import com.campus.secondhand.mapper.SearchHistoryMapper;
import com.campus.secondhand.mapper.UserBehaviorLogMapper;
import com.campus.secondhand.service.impl.RecommendationBehaviorServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationBehaviorServiceTest {

    @Mock
    private SearchHistoryMapper searchHistoryMapper;
    @Mock
    private UserBehaviorLogMapper userBehaviorLogMapper;

    @InjectMocks
    private RecommendationBehaviorServiceImpl recommendationBehaviorService;

    @Test
    void shouldRecordSearchHistoryAndBehaviorLog() {
        recommendationBehaviorService.recordSearch(11L, " iPad ", 2L, new BigDecimal("100"), new BigDecimal("3000"), "price_desc", "public_items");

        ArgumentCaptor<SearchHistory> historyCaptor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryMapper).insert(historyCaptor.capture());
        assertEquals(11L, historyCaptor.getValue().getUserId());
        assertEquals("iPad", historyCaptor.getValue().getKeyword());
        assertEquals(SearchSortType.PRICE_DESC, historyCaptor.getValue().getSortType());

        ArgumentCaptor<UserBehaviorLog> behaviorCaptor = ArgumentCaptor.forClass(UserBehaviorLog.class);
        verify(userBehaviorLogMapper).insert(behaviorCaptor.capture());
        assertEquals(UserBehaviorType.SEARCH, behaviorCaptor.getValue().getBehaviorType());
        assertEquals("public_items", behaviorCaptor.getValue().getSourcePage());
    }

    @Test
    void shouldIgnoreNullUserWhenRecordingView() {
        recommendationBehaviorService.recordItemView(null, 1L, "public_item_detail");

        verify(userBehaviorLogMapper, never()).insert(any(UserBehaviorLog.class));
    }
}

