package com.campus.secondhand.service;

import com.campus.secondhand.vo.publicapi.PublicItemDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicItemPageResponse;

import java.math.BigDecimal;

public interface PublicItemService {

    PublicItemPageResponse listItems(Long userId,
                                     Long categoryId,
                                     String keyword,
                                     String brand,
                                     BigDecimal priceMin,
                                     BigDecimal priceMax,
                                     String conditionLevel,
                                     String tradeMode,
                                     String sortBy,
                                     long page,
                                     long size);

    PublicItemDetailResponse getItemDetail(Long userId, Long itemId);
}
