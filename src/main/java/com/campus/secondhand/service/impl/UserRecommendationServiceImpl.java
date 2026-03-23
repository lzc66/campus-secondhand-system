package com.campus.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.secondhand.common.exception.BusinessException;
import com.campus.secondhand.entity.Item;
import com.campus.secondhand.entity.ItemCategory;
import com.campus.secondhand.entity.ItemImage;
import com.campus.secondhand.entity.SearchHistory;
import com.campus.secondhand.entity.UserBehaviorLog;
import com.campus.secondhand.entity.UserRecommendation;
import com.campus.secondhand.entity.WantedPost;
import com.campus.secondhand.enums.ItemStatus;
import com.campus.secondhand.enums.RecommendationReasonCode;
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
import com.campus.secondhand.service.UserRecommendationService;
import com.campus.secondhand.vo.user.UserRecommendationPageResponse;
import com.campus.secondhand.vo.user.UserRecommendationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRecommendationServiceImpl implements UserRecommendationService {

    private final UserRecommendationMapper userRecommendationMapper;
    private final UserBehaviorLogMapper userBehaviorLogMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final WantedPostMapper wantedPostMapper;
    private final ItemMapper itemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemImageMapper itemImageMapper;
    private final MediaFileMapper mediaFileMapper;

    public UserRecommendationServiceImpl(UserRecommendationMapper userRecommendationMapper,
                                         UserBehaviorLogMapper userBehaviorLogMapper,
                                         SearchHistoryMapper searchHistoryMapper,
                                         WantedPostMapper wantedPostMapper,
                                         ItemMapper itemMapper,
                                         ItemCategoryMapper itemCategoryMapper,
                                         ItemImageMapper itemImageMapper,
                                         MediaFileMapper mediaFileMapper) {
        this.userRecommendationMapper = userRecommendationMapper;
        this.userBehaviorLogMapper = userBehaviorLogMapper;
        this.searchHistoryMapper = searchHistoryMapper;
        this.wantedPostMapper = wantedPostMapper;
        this.itemMapper = itemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.itemImageMapper = itemImageMapper;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public UserRecommendationPageResponse listRecommendations(UserPrincipal principal, long page, long size, boolean refresh) {
        if (refresh || hasNoValidRecommendations(principal.getUserId())) {
            refreshRecommendations(principal, 20);
        }
        Page<UserRecommendation> queryPage = new Page<>(Math.max(page, 1), Math.max(size, 1));
        Page<UserRecommendation> result = userRecommendationMapper.selectPage(queryPage, new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, principal.getUserId())
                .and(q -> q.isNull(UserRecommendation::getExpiresAt).or().gt(UserRecommendation::getExpiresAt, LocalDateTime.now()))
                .orderByDesc(UserRecommendation::getRecommendScore)
                .orderByDesc(UserRecommendation::getGeneratedAt));
        List<UserRecommendationResponse> records = buildResponses(result.getRecords());
        return new UserRecommendationPageResponse(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    @Transactional
    public UserRecommendationPageResponse refreshRecommendations(UserPrincipal principal, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 50));
        LocalDateTime now = LocalDateTime.now();
        Map<Long, BigDecimal> categoryScores = new HashMap<>();
        Map<String, BigDecimal> keywordScores = new HashMap<>();
        Set<Long> excludeItemIds = new HashSet<>();

        List<UserBehaviorLog> behaviors = userBehaviorLogMapper.selectList(new LambdaQueryWrapper<UserBehaviorLog>()
                .eq(UserBehaviorLog::getUserId, principal.getUserId())
                .ge(UserBehaviorLog::getOccurredAt, now.minusDays(90))
                .orderByDesc(UserBehaviorLog::getOccurredAt));
        List<Long> behaviorItemIds = behaviors.stream().map(UserBehaviorLog::getItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Item> behaviorItemMap = behaviorItemIds.isEmpty() ? Map.of() : itemMapper.selectBatchIds(behaviorItemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        for (UserBehaviorLog behavior : behaviors) {
            BigDecimal weight = behavior.getWeight() == null ? BigDecimal.ONE : behavior.getWeight();
            if (behavior.getItemId() != null) {
                Item item = behaviorItemMap.get(behavior.getItemId());
                if (item != null && item.getCategoryId() != null) {
                    categoryScores.merge(item.getCategoryId(), weight, BigDecimal::add);
                }
                if (item != null) {
                    collectKeywords(keywordScores, item.getTitle(), weight);
                    collectKeywords(keywordScores, item.getBrand(), weight);
                    collectKeywords(keywordScores, item.getModel(), weight);
                }
                if (behavior.getBehaviorType() == UserBehaviorType.PURCHASE) {
                    excludeItemIds.add(behavior.getItemId());
                }
            }
            collectKeywords(keywordScores, behavior.getSearchKeyword(), weight);
        }

        List<SearchHistory> searches = searchHistoryMapper.selectList(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, principal.getUserId())
                .ge(SearchHistory::getSearchedAt, now.minusDays(60))
                .orderByDesc(SearchHistory::getSearchedAt));
        for (SearchHistory search : searches) {
            if (search.getCategoryId() != null) {
                categoryScores.merge(search.getCategoryId(), new BigDecimal("1.50"), BigDecimal::add);
            }
            collectKeywords(keywordScores, search.getKeyword(), new BigDecimal("1.20"));
        }

        List<WantedPost> wantedPosts = wantedPostMapper.selectList(new LambdaQueryWrapper<WantedPost>()
                .eq(WantedPost::getRequesterUserId, principal.getUserId())
                .isNull(WantedPost::getDeletedAt)
                .ge(WantedPost::getCreatedAt, now.minusDays(120))
                .orderByDesc(WantedPost::getCreatedAt));
        for (WantedPost wantedPost : wantedPosts) {
            if (wantedPost.getCategoryId() != null) {
                categoryScores.merge(wantedPost.getCategoryId(), new BigDecimal("4.00"), BigDecimal::add);
            }
            collectKeywords(keywordScores, wantedPost.getTitle(), new BigDecimal("2.00"));
            collectKeywords(keywordScores, wantedPost.getBrand(), new BigDecimal("1.50"));
            collectKeywords(keywordScores, wantedPost.getModel(), new BigDecimal("1.50"));
        }

        List<Item> candidates = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, ItemStatus.ON_SALE)
                .gt(Item::getStock, 0)
                .ne(Item::getSellerUserId, principal.getUserId())
                .orderByDesc(Item::getPublishedAt));

        List<ScoredRecommendation> scored = new ArrayList<>();
        for (Item item : candidates) {
            if (excludeItemIds.contains(item.getItemId())) {
                continue;
            }
            BigDecimal categoryScore = categoryScores.getOrDefault(item.getCategoryId(), BigDecimal.ZERO);
            BigDecimal keywordScore = calculateKeywordScore(item, keywordScores);
            BigDecimal popularityScore = calculatePopularityScore(item, now);
            BigDecimal totalScore = categoryScore.multiply(new BigDecimal("1.40"))
                    .add(keywordScore.multiply(new BigDecimal("1.20")))
                    .add(popularityScore);
            RecommendationReasonCode reasonCode = resolveReasonCode(categoryScore, keywordScore, item, behaviors);
            if (totalScore.compareTo(BigDecimal.ZERO) > 0) {
                scored.add(new ScoredRecommendation(item, totalScore.setScale(4, RoundingMode.HALF_UP), reasonCode));
            }
        }

        if (scored.isEmpty()) {
            candidates.stream()
                    .sorted(Comparator.comparing((Item item) -> calculatePopularityScore(item, now)).reversed())
                    .limit(effectiveLimit)
                    .forEach(item -> scored.add(new ScoredRecommendation(
                            item,
                            calculatePopularityScore(item, now).setScale(4, RoundingMode.HALF_UP),
                            RecommendationReasonCode.HOT_SALE
                    )));
        }

        scored.sort(Comparator.comparing(ScoredRecommendation::score).reversed()
                .thenComparing(r -> r.item().getPublishedAt(), Comparator.nullsLast(Comparator.reverseOrder())));
        List<ScoredRecommendation> top = scored.stream().limit(effectiveLimit).toList();

        userRecommendationMapper.delete(new LambdaQueryWrapper<UserRecommendation>().eq(UserRecommendation::getUserId, principal.getUserId()));
        LocalDateTime expiresAt = now.plusDays(3);
        for (ScoredRecommendation recommendation : top) {
            userRecommendationMapper.insert(UserRecommendation.builder()
                    .userId(principal.getUserId())
                    .itemId(recommendation.item().getItemId())
                    .recommendScore(recommendation.score())
                    .reasonCode(recommendation.reasonCode())
                    .isClicked(0)
                    .generatedAt(now)
                    .expiresAt(expiresAt)
                    .build());
        }
        Page<UserRecommendation> refreshedPage = new Page<>(1, effectiveLimit);
        Page<UserRecommendation> refreshedResult = userRecommendationMapper.selectPage(refreshedPage, new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, principal.getUserId())
                .and(q -> q.isNull(UserRecommendation::getExpiresAt).or().gt(UserRecommendation::getExpiresAt, LocalDateTime.now()))
                .orderByDesc(UserRecommendation::getRecommendScore)
                .orderByDesc(UserRecommendation::getGeneratedAt));
        return new UserRecommendationPageResponse(refreshedResult.getCurrent(), refreshedResult.getSize(), refreshedResult.getTotal(), buildResponses(refreshedResult.getRecords()));
    }

    @Override
    @Transactional
    public void markClicked(UserPrincipal principal, Long recommendationId) {
        UserRecommendation recommendation = userRecommendationMapper.selectById(recommendationId);
        if (recommendation == null || !Objects.equals(recommendation.getUserId(), principal.getUserId())) {
            throw new BusinessException(40495, HttpStatus.NOT_FOUND, "Recommendation not found");
        }
        recommendation.setIsClicked(1);
        userRecommendationMapper.updateById(recommendation);
    }

    private boolean hasNoValidRecommendations(Long userId) {
        Long count = userRecommendationMapper.selectCount(new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getUserId, userId)
                .and(q -> q.isNull(UserRecommendation::getExpiresAt).or().gt(UserRecommendation::getExpiresAt, LocalDateTime.now())));
        return count == null || count == 0;
    }

    private List<UserRecommendationResponse> buildResponses(List<UserRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = recommendations.stream().map(UserRecommendation::getItemId).distinct().toList();
        Map<Long, Item> itemMap = itemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(Item::getItemId, item -> item));
        List<Long> categoryIds = itemMap.values().stream().map(Item::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ItemCategory> categoryMap = categoryIds.isEmpty() ? Map.of() : itemCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ItemCategory::getCategoryId, category -> category));
        Map<Long, String> coverImageMap = loadCoverImageUrls(itemIds);
        return recommendations.stream()
                .map(rec -> {
                    Item item = itemMap.get(rec.getItemId());
                    if (item == null) {
                        return null;
                    }
                    ItemCategory category = categoryMap.get(item.getCategoryId());
                    return new UserRecommendationResponse(
                            rec.getRecommendationId(),
                            item.getItemId(),
                            item.getCategoryId(),
                            category == null ? null : category.getCategoryName(),
                            item.getTitle(),
                            item.getBrand(),
                            item.getModel(),
                            coverImageMap.get(item.getItemId()),
                            item.getPrice(),
                            rec.getReasonCode().getValue(),
                            rec.getRecommendScore(),
                            Objects.equals(rec.getIsClicked(), 1),
                            item.getViewCount(),
                            item.getCommentCount(),
                            rec.getGeneratedAt(),
                            rec.getExpiresAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, String> loadCoverImageUrls(List<Long> itemIds) {
        List<ItemImage> images = itemImageMapper.selectList(new LambdaQueryWrapper<ItemImage>()
                .in(ItemImage::getItemId, itemIds)
                .orderByAsc(ItemImage::getSortOrder));
        Map<Long, ItemImage> coverMap = new HashMap<>();
        for (ItemImage image : images) {
            coverMap.putIfAbsent(image.getItemId(), image);
            if (Objects.equals(image.getIsCover(), 1)) {
                coverMap.put(image.getItemId(), image);
            }
        }
        List<Long> fileIds = coverMap.values().stream().map(ItemImage::getFileId).distinct().toList();
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> fileUrlMap = mediaFileMapper.selectBatchIds(fileIds).stream()
                .collect(Collectors.toMap(media -> media.getFileId(), media -> media.getFileUrl()));
        Map<Long, String> result = new HashMap<>();
        coverMap.forEach((itemId, image) -> result.put(itemId, fileUrlMap.get(image.getFileId())));
        return result;
    }

    private BigDecimal calculateKeywordScore(Item item, Map<String, BigDecimal> keywordScores) {
        BigDecimal score = BigDecimal.ZERO;
        String haystack = (safe(item.getTitle()) + " " + safe(item.getBrand()) + " " + safe(item.getModel()) + " " + safe(item.getDescription())).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, BigDecimal> entry : keywordScores.entrySet()) {
            if (haystack.contains(entry.getKey())) {
                score = score.add(entry.getValue());
            }
        }
        return score;
    }

    private BigDecimal calculatePopularityScore(Item item, LocalDateTime now) {
        BigDecimal score = BigDecimal.valueOf(item.getViewCount() == null ? 0 : item.getViewCount()).multiply(new BigDecimal("0.02"))
                .add(BigDecimal.valueOf(item.getCommentCount() == null ? 0 : item.getCommentCount()).multiply(new BigDecimal("0.15")));
        if (item.getPublishedAt() != null && item.getPublishedAt().isAfter(now.minusDays(14))) {
            score = score.add(new BigDecimal("1.20"));
        }
        return score;
    }

    private RecommendationReasonCode resolveReasonCode(BigDecimal categoryScore,
                                                       BigDecimal keywordScore,
                                                       Item item,
                                                       List<UserBehaviorLog> behaviors) {
        if (keywordScore.compareTo(categoryScore) > 0 && keywordScore.compareTo(BigDecimal.ZERO) > 0) {
            return RecommendationReasonCode.KEYWORD_MATCH;
        }
        boolean recentlyViewed = behaviors.stream().anyMatch(log -> Objects.equals(log.getItemId(), item.getItemId()) && log.getBehaviorType() == UserBehaviorType.VIEW);
        if (recentlyViewed) {
            return RecommendationReasonCode.RECENT_VIEW;
        }
        if (categoryScore.compareTo(BigDecimal.ZERO) > 0) {
            return RecommendationReasonCode.SIMILAR_CATEGORY;
        }
        return RecommendationReasonCode.HOT_SALE;
    }

    private void collectKeywords(Map<String, BigDecimal> keywordScores, String rawText, BigDecimal weight) {
        if (!StringUtils.hasText(rawText)) {
            return;
        }
        for (String token : rawText.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            if (token.isBlank() || token.length() < 2) {
                continue;
            }
            keywordScores.merge(token, weight, BigDecimal::add);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredRecommendation(Item item, BigDecimal score, RecommendationReasonCode reasonCode) {
    }
}

