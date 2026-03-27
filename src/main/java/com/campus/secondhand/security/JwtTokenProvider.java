package com.campus.secondhand.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAdminToken(Long adminId, String adminNo, String roleCode, String accountStatus) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", adminId);
        claims.put("adminNo", adminNo);
        claims.put("roleCode", roleCode);
        claims.put("accountStatus", accountStatus);
        return createToken(adminNo, "admin", adminId, claims);
    }

    public String createUserToken(Long userId, String studentNo, String accountStatus) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("studentNo", studentNo);
        claims.put("accountStatus", accountStatus);
        return createToken(studentNo, "user", userId, claims);
    }

    private String createToken(String subject, String accountType, Long accountId, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getExpirationSeconds());

        return Jwts.builder()
                .subject(subject)
                .claim("accountType", accountType)
                .claim("accountId", accountId)
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }
}