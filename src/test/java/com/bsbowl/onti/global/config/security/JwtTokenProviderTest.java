package com.bsbowl.onti.global.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long-1234567890",
                86400000L
        );
    }

    @Test
    void generateToken_thenGetUserId_returnsOriginalUserId() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("user-123");
    }

    @Test
    void isValid_forTamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateToken("user-123");

        assertThat(jwtTokenProvider.isValid(token + "tampered")).isFalse();
    }

    @Test
    void isValid_forExpiredToken_returnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-must-be-at-least-32-bytes-long-1234567890",
                -1000L
        );
        String token = shortLived.generateToken("user-123");

        assertThat(shortLived.isValid(token)).isFalse();
    }
}
