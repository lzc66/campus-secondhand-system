package com.campus.secondhand.service;

import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.user.UserRecommendationPageResponse;

public interface UserRecommendationService {

    UserRecommendationPageResponse listRecommendations(UserPrincipal principal, long page, long size, boolean refresh);

    UserRecommendationPageResponse refreshRecommendations(UserPrincipal principal, int limit);

    void markClicked(UserPrincipal principal, Long recommendationId);
}
