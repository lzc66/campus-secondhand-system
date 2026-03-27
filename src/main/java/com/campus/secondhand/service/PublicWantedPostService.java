package com.campus.secondhand.service;

import com.campus.secondhand.vo.publicapi.PublicWantedPostDetailResponse;
import com.campus.secondhand.vo.publicapi.PublicWantedPostPageResponse;

import java.math.BigDecimal;

public interface PublicWantedPostService {

    PublicWantedPostPageResponse listWantedPosts(Long categoryId,
                                                 String keyword,
                                                 String brand,
                                                 BigDecimal priceMin,
                                                 BigDecimal priceMax,
                                                 String sortBy,
                                                 long page,
                                                 long size);

    PublicWantedPostDetailResponse getWantedPostDetail(Long wantedPostId);
}
