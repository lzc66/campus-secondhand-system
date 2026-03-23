package com.campus.secondhand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.UserBehaviorLog;
import com.campus.secondhand.entity.UserRecommendation;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.RecommendationReasonCode;
import com.campus.secondhand.enums.SearchSortType;
import com.campus.secondhand.enums.UserAccountStatus;
import com.campus.secondhand.enums.UserBehaviorType;
import com.campus.secondhand.mapper.ItemCategoryMapper;
import com.campus.secondhand.mapper.ItemImageMapper;
import com.campus.secondhand.mapper.ItemMapper;
import com.campus.secondhand.mapper.MediaFileMapper;
import com.campus.secondhand.mapper.SearchHistoryMapper;
import com.campus.secondhand.mapper.UserBehaviorLogMapper;
import com.campus.secondhand.mapper.UserRecommendationMapper;
import com.campus.secondhand.mapper.WantedPostMapper;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.service.impl.UserRecommendationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRecommendationServiceTest {

    @Mock
    private UserRecommendationMapper userRecommendationMapper;
    @Mock
    private UserBehaviorLogMapper userBehaviorLogMapper;
    @Mock
    private SearchHistoryMapper searchHistoryMapper;
    @Mock
    private WantedPostMapper wantedPostMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemCategoryMapper itemCategoryMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private MediaFileMapper mediaFileMapper;

    @Test
    void shouldRefreshRecommendationsBasedOnBehaviorSignals() {
        UserRecommendationServiceImpl service = new UserRecommendationServiceImpl(
                userRecommendationMapper,
                userBehaviorLogMapper,
                searchHistoryMapper,
                wantedPostMapper,
                itemMapper,
                itemCategoryMapper,
                itemImageMapper,
                mediaFileMapper
        );
        UserPrincipal principal = new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        Item viewedItem = Item.builder().itemId(10L).categoryId(1L).title("iPad Pro").brand("Apple").model("M2").status(ItemStatus.ON_SALE).build();
        Item purchasedItem = Item.builder().itemId(11L).categoryId(2L).title("Road Bike").brand("Giant").model("Escape").status(ItemStatus.ON_SALE).build();
        Item recommendedItem = Item.builder()
                .itemId(20L)
                .sellerUserId(22L)
                .categoryId(1L)
                .title("iPad Mini")
                .brand("Apple")
                .model("A17")
                .description("Almost new iPad mini")
                .price(new BigDecimal("2399.00"))
                .stock(1)
                .status(ItemStatus.ON_SALE)
                .viewCount(60)
                .commentCount(3)
                .publishedAt(now.minusDays(1))
                .build();
        Item ownItem = Item.builder().itemId(21L).sellerUserId(11L).categoryId(1L).title("My Item").price(new BigDecimal("10.00")).stock(1).status(ItemStatus.ON_SALE).publishedAt(now.minusDays(1)).build();
        Item lowScoreItem = Item.builder()
                .itemId(22L)
                .sellerUserId(33L)
                .categoryId(3L)
                .title("Scientific Calculator")
                .brand("Casio")
                .model("991CN")
                .description("Good condition")
                .price(new BigDecimal("88.00"))
                .stock(1)
                .status(ItemStatus.ON_SALE)
                .viewCount(1)
                .commentCount(0)
                .publishedAt(now.minusDays(30))
                .build();

        when(userBehaviorLogMapper.selectList(any())).thenReturn(List.of(
                UserBehaviorLog.builder().userId(11L).itemId(10L).behaviorType(UserBehaviorType.VIEW).weight(new BigDecimal("1.0")).occurredAt(now.minusDays(1)).build(),
                UserBehaviorLog.builder().userId(11L).itemId(11L).behaviorType(UserBehaviorType.PURCHASE).weight(new BigDecimal("5.0")).occurredAt(now.minusDays(2)).build()
        ));
        when(searchHistoryMapper.selectList(any())).thenReturn(List.of(
                SearchHistory.builder().userId(11L).categoryId(1L).keyword("iPad").sortType(SearchSortType.LATEST).searchedAt(now.minusDays(2)).build()
        ));
        when(wantedPostMapper.selectList(any())).thenReturn(List.of(
                WantedPost.builder().requesterUserId(11L).categoryId(1L).title("Need iPad").createdAt(now.minusDays(3)).build()
        ));
        when(itemMapper.selectList(any())).thenReturn(List.of(recommendedItem, purchasedItem, ownItem, lowScoreItem));
        when(itemImageMapper.selectList(any())).thenReturn(List.of());

        Map<Long, Item> itemMap = Map.of(10L, viewedItem, 11L, purchasedItem, 20L, recommendedItem, 21L, ownItem, 22L, lowScoreItem);
        when(itemMapper.selectBatchIds(any())).thenAnswer(invocation -> toItems(invocation.getArgument(0), itemMap));

        Map<Long, ItemCategory> categories = Map.of(
                1L, ItemCategory.builder().categoryId(1L).categoryName("Digital Devices").build(),
                3L, ItemCategory.builder().categoryId(3L).categoryName("Study Supplies").build()
        );
        when(itemCategoryMapper.selectBatchIds(any())).thenAnswer(invocation -> toItems(invocation.getArgument(0), categories));

        List<UserRecommendation> inserted = new ArrayList<>();
        doAnswer(invocation -> {
            inserted.clear();
            return 1;
        }).when(userRecommendationMapper).delete(any());
        doAnswer(invocation -> {
            UserRecommendation recommendation = invocation.getArgument(0);
            recommendation.setRecommendationId((long) inserted.size() + 1);
            inserted.add(recommendation);
            return 1;
        }).when(userRecommendationMapper).insert(any(UserRecommendation.class));
        when(userRecommendationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<UserRecommendation> page = invocation.getArgument(0);
            page.setRecords(new ArrayList<>(inserted));
            page.setTotal(inserted.size());
            return page;
        });

        var response = service.refreshRecommendations(principal, 10);

        assertFalse(response.records().isEmpty());
        assertEquals(20L, response.records().get(0).itemId());
        assertEquals(RecommendationReasonCode.SIMILAR_CATEGORY.getValue(), response.records().get(0).reasonCode());
        verify(userRecommendationMapper).delete(any());
    }

    @Test
    void shouldMarkRecommendationClicked() {
        UserRecommendationServiceImpl service = new UserRecommendationServiceImpl(
                userRecommendationMapper,
                userBehaviorLogMapper,
                searchHistoryMapper,
                wantedPostMapper,
                itemMapper,
                itemCategoryMapper,
                itemImageMapper,
                mediaFileMapper
        );
        UserPrincipal principal = new UserPrincipal(11L, "20240001", "Alice", "alice@campus.local", UserAccountStatus.ACTIVE);
        when(userRecommendationMapper.selectById(5L)).thenReturn(UserRecommendation.builder()
                .recommendationId(5L)
                .userId(11L)
                .itemId(20L)
                .recommendScore(new BigDecimal("8.8"))
                .reasonCode(RecommendationReasonCode.HOT_SALE)
                .isClicked(0)
                .build());

        service.markClicked(principal, 5L);

        ArgumentCaptor<UserRecommendation> captor = ArgumentCaptor.forClass(UserRecommendation.class);
        verify(userRecommendationMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsClicked());
    }

    private <T> List<T> toItems(Collection<Long> ids, Map<Long, T> source) {
        return ids.stream().map(source::get).filter(java.util.Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }
}


