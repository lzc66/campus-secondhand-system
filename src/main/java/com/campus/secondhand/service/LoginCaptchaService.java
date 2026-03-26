package com.campus.secondhand.service;

import com.campus.secondhand.vo.user.UserCaptchaResponse;

public interface LoginCaptchaService {

    UserCaptchaResponse generateUserLoginCaptcha();

    boolean verifyUserLoginCaptcha(String captchaKey, String captcha);

    UserCaptchaResponse generateAdminLoginCaptcha();

    boolean verifyAdminLoginCaptcha(String captchaKey, String captcha);
}