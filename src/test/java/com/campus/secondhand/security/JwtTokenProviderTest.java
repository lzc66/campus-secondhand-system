package com.campus.secondhand.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void shouldCreateAndParseAdminToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("UnitTestOnlyJwtSecretKey-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        properties.setExpirationSeconds(3600);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.createAdminToken(1L, "admin1001", "super_admin", "active");
        Claims claims = provider.parseClaims(token);

        assertTrue(provider.isValid(token));
        assertEquals("admin1001", claims.getSubject());
        assertEquals("admin", claims.get("accountType", String.class));
        assertEquals("1", claims.get("accountId").toString());
        assertEquals("super_admin", claims.get("roleCode", String.class));
    }

    @Test
    void shouldRejectLeakedDefaultSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("CampusSecondhandJwtSecretKeyForAdminInit1234567890");
        properties.setExpirationSeconds(3600);

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(properties));
    }

    @Test
    void shouldRejectShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        properties.setExpirationSeconds(3600);

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(properties));
    }
}