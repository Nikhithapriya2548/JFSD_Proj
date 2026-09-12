package com.omnishop.user.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-32!!";

    @Test
    void roundTripPreservesUserIdAndRole() {
        JwtUtil jwt = new JwtUtil(SECRET, 60000);
        String token = jwt.generateToken(42L, "ADMIN");
        assertThat(jwt.validate(token)).isTrue();
        assertThat(jwt.userId(token)).isEqualTo(42L);
        assertThat(jwt.role(token)).isEqualTo("ADMIN");
    }

    @Test
    void tamperedTokenRejected() {
        JwtUtil jwt = new JwtUtil(SECRET, 60000);
        String token = jwt.generateToken(1L, "CUSTOMER") + "x";
        assertThat(jwt.validate(token)).isFalse();
    }

    @Test
    void expiredTokenRejected() {
        JwtUtil jwt = new JwtUtil(SECRET, -1000);
        assertThat(jwt.validate(jwt.generateToken(1L, "CUSTOMER"))).isFalse();
    }

    @Test
    void wrongSecretRejected() {
        String token = new JwtUtil(SECRET, 60000).generateToken(1L, "CUSTOMER");
        assertThat(new JwtUtil("a-different-secret-key-that-is-32ch!!", 60000)
                .validate(token)).isFalse();
    }
}
