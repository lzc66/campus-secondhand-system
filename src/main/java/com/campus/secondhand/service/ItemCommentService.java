package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.CreateItemCommentRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.publicapi.PublicItemCommentPageResponse;
import com.campus.secondhand.vo.publicapi.PublicItemCommentResponse;
import com.campus.secondhand.vo.user.UserReceivedCommentPageResponse;

public interface ItemCommentService {

    PublicItemCommentPageResponse listPublicComments(Long itemId, long page, long size);

    PublicItemCommentResponse createComment(UserPrincipal principal, Long itemId, CreateItemCommentRequest request);

    PublicItemCommentResponse replyComment(UserPrincipal principal, Long commentId, CreateItemCommentRequest request);

    UserReceivedCommentPageResponse listReceivedComments(UserPrincipal principal, Long itemId, long page, long size);
}