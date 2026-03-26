package com.campus.secondhand.service;

import com.campus.secondhand.dto.user.UserLoginRequest;
import com.campus.secondhand.security.UserPrincipal;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import com.campus.secondhand.vo.user.UserLoginResponse;
import com.campus.secondhand.vo.user.UserProfileResponse;

public interface UserAuthService {

    UserCaptchaResponse getLoginCaptcha();

    UserLoginResponse login(UserLoginRequest request, String ipAddress, String userAgent);

    UserProfileResponse getCurrentUser(UserPrincipal principal);
}