package com.campus.secondhand.service.impl;

import com.campus.secondhand.service.LoginCaptchaService;
import com.campus.secondhand.vo.user.UserCaptchaResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginCaptchaServiceImpl implements LoginCaptchaService {

    private static final String CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CAPTCHA_LENGTH = 4;
    private static final long EXPIRES_IN_SECONDS = 180L;
    private static final String USER_PREFIX = "user:";
    private static final String ADMIN_PREFIX = "admin:";

    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    public LoginCaptchaServiceImpl() {
        this(new SecureRandom(), Clock.systemDefaultZone());
    }

    public LoginCaptchaServiceImpl(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Override
    public UserCaptchaResponse generateUserLoginCaptcha() {
        return generateCaptcha(USER_PREFIX, false);
    }

    @Override
    public boolean verifyUserLoginCaptcha(String captchaKey, String captcha) {
        return verifyCaptcha(USER_PREFIX, captchaKey, captcha);
    }

    @Override
    public UserCaptchaResponse generateAdminLoginCaptcha() {
        return generateCaptcha(ADMIN_PREFIX, true);
    }

    @Override
    public boolean verifyAdminLoginCaptcha(String captchaKey, String captcha) {
        return verifyCaptcha(ADMIN_PREFIX, captchaKey, captcha);
    }

    private UserCaptchaResponse generateCaptcha(String prefix, boolean adminStyle) {
        cleanupExpired();
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        String captchaText = randomCaptchaText();
        Instant expiresAt = Instant.now(clock).plusSeconds(EXPIRES_IN_SECONDS);
        captchaStore.put(prefix + captchaKey, new CaptchaEntry(captchaText, expiresAt));
        return new UserCaptchaResponse(captchaKey, buildImageData(captchaText, adminStyle), EXPIRES_IN_SECONDS);
    }

    private boolean verifyCaptcha(String prefix, String captchaKey, String captcha) {
        cleanupExpired();
        if (captchaKey == null || captcha == null) {
            return false;
        }
        CaptchaEntry entry = captchaStore.remove(prefix + captchaKey.trim());
        if (entry == null || entry.expiresAt().isBefore(Instant.now(clock))) {
            return false;
        }
        return entry.captchaText().equalsIgnoreCase(captcha.trim());
    }

    private String randomCaptchaText() {
        StringBuilder builder = new StringBuilder(CAPTCHA_LENGTH);
        for (int index = 0; index < CAPTCHA_LENGTH; index++) {
            builder.append(CAPTCHA_CHARS.charAt(secureRandom.nextInt(CAPTCHA_CHARS.length())));
        }
        return builder.toString();
    }

    private void cleanupExpired() {
        Instant now = Instant.now(clock);
        captchaStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String buildImageData(String captchaText, boolean adminStyle) {
        String backgroundStart = adminStyle ? "#edf2fb" : "#f4efe5";
        String backgroundEnd = adminStyle ? "#dce7f5" : "#e3eddc";
        String borderColor = adminStyle ? "#1f3a5f" : "#103b2e";
        String accentColor = adminStyle ? "#c55d1f" : "#d5752a";
        String primaryText = adminStyle ? "#183153" : "#123f31";
        String secondaryText = adminStyle ? "#20406b" : "#1f3a5f";

        StringBuilder builder = new StringBuilder();
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"132\" height=\"44\" viewBox=\"0 0 132 44\">");
        builder.append("<defs><linearGradient id=\"bg\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">")
                .append("<stop offset=\"0%\" stop-color=\"").append(backgroundStart).append("\"/>")
                .append("<stop offset=\"100%\" stop-color=\"").append(backgroundEnd).append("\"/></linearGradient></defs>");
        builder.append("<rect width=\"132\" height=\"44\" rx=\"12\" fill=\"url(#bg)\"/>");
        builder.append("<rect x=\"1\" y=\"1\" width=\"130\" height=\"42\" rx=\"11\" fill=\"none\" stroke=\"")
                .append(borderColor)
                .append("\" stroke-opacity=\"0.12\"/>");

        for (int index = 0; index < 5; index++) {
            int x1 = 6 + secureRandom.nextInt(120);
            int y1 = 4 + secureRandom.nextInt(36);
            int x2 = 6 + secureRandom.nextInt(120);
            int y2 = 4 + secureRandom.nextInt(36);
            builder.append("<line x1=\"").append(x1)
                    .append("\" y1=\"").append(y1)
                    .append("\" x2=\"").append(x2)
                    .append("\" y2=\"").append(y2)
                    .append("\" stroke=\"").append(accentColor).append("\" stroke-width=\"1.2\" stroke-opacity=\"0.35\"/>");
        }

        for (int index = 0; index < 16; index++) {
            int cx = 8 + secureRandom.nextInt(116);
            int cy = 6 + secureRandom.nextInt(32);
            int radius = 1 + secureRandom.nextInt(2);
            String fill = index % 2 == 0 ? borderColor : accentColor;
            builder.append("<circle cx=\"").append(cx)
                    .append("\" cy=\"").append(cy)
                    .append("\" r=\"").append(radius)
                    .append("\" fill=\"").append(fill)
                    .append("\" fill-opacity=\"0.22\"/>");
        }

        int[] xPositions = {18, 45, 72, 99};
        for (int index = 0; index < captchaText.length(); index++) {
            int rotate = secureRandom.nextInt(21) - 10;
            int y = 29 + secureRandom.nextInt(6);
            String fill = index % 2 == 0 ? primaryText : secondaryText;
            builder.append("<text x=\"").append(xPositions[index])
                    .append("\" y=\"").append(y)
                    .append("\" font-family=\"Georgia, 'Times New Roman', serif\" font-size=\"24\" font-weight=\"700\" fill=\"")
                    .append(fill)
                    .append("\" transform=\"rotate(")
                    .append(rotate)
                    .append(' ')
                    .append(xPositions[index])
                    .append(' ')
                    .append(y)
                    .append(")\">")
                    .append(captchaText.charAt(index))
                    .append("</text>");
        }

        builder.append("</svg>");
        return "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private record CaptchaEntry(String captchaText, Instant expiresAt) {
    }
}