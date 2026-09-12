package com.omnishop.user.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    void fiveAttemptsAllowedSixthBlocked() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed("a@x.com");
            limiter.recordFailure("a@x.com");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("a@x.com"))
                .isInstanceOf(LoginRateLimiter.TooManyAttemptsException.class);
    }

    @Test
    void successResetsCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed("b@x.com");
            limiter.recordFailure("b@x.com");
        }
        limiter.recordSuccess("b@x.com");
        limiter.checkAllowed("b@x.com"); // no throw
    }

    @Test
    void keysAreIndependent() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 5; i++) {
            limiter.checkAllowed("c@x.com");
            limiter.recordFailure("c@x.com");
        }
        limiter.checkAllowed("other@x.com"); // no throw
    }
}
