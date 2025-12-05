package com.homesweet.sse.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String secretKey = "thisisthesecretkeyfortestingpurposesonlydontuseinproduction";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey);
    }

    @Test
    void validateAndGetUserId_ValidToken_ReturnsUserId() {
        String userId = "12345";
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Long result = jwtTokenProvider.validateAndGetUserId(token);

        assertNotNull(result);
        assertEquals(Long.parseLong(userId), result);
    }

    @Test
    void validateAndGetUserId_InvalidToken_ReturnsNull() {
        String token = "invalid.token.string";

        Long result = jwtTokenProvider.validateAndGetUserId(token);

        assertNull(result);
    }

    @Test
    void validateAndGetUserId_ExpiredToken_ReturnsNull() {
        String userId = "12345";
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Long result = jwtTokenProvider.validateAndGetUserId(token);

        assertNull(result);
    }
}
