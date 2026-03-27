package com.campus.secondhand.service;

import java.math.BigDecimal;

public interface RecommendationBehaviorService {

    void recordSearch(Long userId,
                      String keyword,
                      Long categoryId,
                      BigDecimal minPrice,
                      BigDecimal maxPrice,
                      String sortBy,
                      String sourcePage);

    void recordItemView(Long userId, Long itemId, String sourcePage);

    void recordWantedPostPublish(Long userId, Long wantedPostId, String title);

    void recordPurchase(Long userId, Long itemId, Long orderId);
}
