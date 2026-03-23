package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.ChangePasswordRequest;
import com.campus.secondhand.dto.user.UpdateUserProfileRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.common.MediaFileResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileResponse getProfile(UserPrincipal principal);

    UserProfileResponse updateProfile(UserPrincipal principal, UpdateUserProfileRequest request);

    void changePassword(UserPrincipal principal, ChangePasswordRequest request);

    MediaFileResponse uploadAvatar(UserPrincipal principal, MultipartFile file);
}