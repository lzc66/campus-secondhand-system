package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.SaveItemRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.UserItemDetailResponse;
import com.campus.secondhand.vo.user.UserItemPageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserItemService {

    MediaFileResponse uploadItemImage(UserPrincipal principal, MultipartFile file);

    UserItemDetailResponse createItem(UserPrincipal principal, SaveItemRequest request);

    UserItemDetailResponse updateItem(UserPrincipal principal, Long itemId, SaveItemRequest request);

    UserItemPageResponse listMyItems(UserPrincipal principal, String status, long page, long size);

    UserItemDetailResponse getMyItem(UserPrincipal principal, Long itemId);

    void deleteItem(UserPrincipal principal, Long itemId);
}