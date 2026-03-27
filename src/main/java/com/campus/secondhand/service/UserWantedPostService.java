package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.SaveWantedPostRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.user.UserWantedPostDetailResponse;
import com.campus.secondhand.vo.user.UserWantedPostPageResponse;

public interface UserWantedPostService {

    UserWantedPostDetailResponse createWantedPost(UserPrincipal principal, SaveWantedPostRequest request);

    UserWantedPostDetailResponse updateWantedPost(UserPrincipal principal, Long wantedPostId, SaveWantedPostRequest request);

    UserWantedPostPageResponse listMyWantedPosts(UserPrincipal principal, String status, long page, long size);

    UserWantedPostDetailResponse getMyWantedPost(UserPrincipal principal, Long wantedPostId);

    void deleteWantedPost(UserPrincipal principal, Long wantedPostId);
}
