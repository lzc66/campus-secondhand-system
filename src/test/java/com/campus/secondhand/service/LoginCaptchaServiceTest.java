package com.campus.secondhand.service;

import com.campus.secondhand.service.impl.LoginCaptchaServiceImpl;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginCaptchaServiceTest {

    @Test
    void shouldGenerateAndVerifyCaptchaSuccessfully() {
        LoginCaptchaServiceImpl captchaService = new LoginCaptchaServiceImpl(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-03-26T06:00:00Z"), ZoneOffset.UTC)
        );

        UserCaptchaResponse response = captchaService.generateUserLoginCaptcha();

        assertNotNull(response.captchaKey());
        assertTrue(response.imageData().startsWith("data:image/svg+xml;base64,"));
        assertTrue(captchaService.verifyUserLoginCaptcha(response.captchaKey(), decodeCaptcha(response.imageData()).toLowerCase()));
    }

    @Test
    void shouldRejectCaptchaAfterReuse() {
        LoginCaptchaServiceImpl captchaService = new LoginCaptchaServiceImpl(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-03-26T06:00:00Z"), ZoneOffset.UTC)
        );

        UserCaptchaResponse response = captchaService.generateUserLoginCaptcha();
        String captchaText = decodeCaptcha(response.imageData());

        assertTrue(captchaService.verifyUserLoginCaptcha(response.captchaKey(), captchaText));
        assertFalse(captchaService.verifyUserLoginCaptcha(response.captchaKey(), captchaText));
    }

    @Test
    void shouldRejectWrongCaptcha() {
        LoginCaptchaServiceImpl captchaService = new LoginCaptchaServiceImpl(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-03-26T06:00:00Z"), ZoneOffset.UTC)
        );

        UserCaptchaResponse response = captchaService.generateUserLoginCaptcha();

        assertFalse(captchaService.verifyUserLoginCaptcha(response.captchaKey(), "ABCD"));
    }

    @Test
    void shouldKeepAdminAndUserCaptchaSeparated() {
        LoginCaptchaServiceImpl captchaService = new LoginCaptchaServiceImpl(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-03-26T06:00:00Z"), ZoneOffset.UTC)
        );

        UserCaptchaResponse userCaptcha = captchaService.generateUserLoginCaptcha();
        UserCaptchaResponse adminCaptcha = captchaService.generateAdminLoginCaptcha();

        assertFalse(captchaService.verifyAdminLoginCaptcha(userCaptcha.captchaKey(), decodeCaptcha(userCaptcha.imageData())));
        assertTrue(captchaService.verifyAdminLoginCaptcha(adminCaptcha.captchaKey(), decodeCaptcha(adminCaptcha.imageData())));
    }

    private String decodeCaptcha(String imageData) {
        String encoded = imageData.substring("data:image/svg+xml;base64,".length());
        String svg = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile(">(\\w)</text>").matcher(svg);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            builder.append(matcher.group(1));
        }
        return builder.toString();
    }
}