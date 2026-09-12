package com.omnishop.order.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class OrderCreationRateLimitFilterTest {

    private final OrderCreationRateLimitFilter filter = new OrderCreationRateLimitFilter();

    private MockHttpServletRequest postOrder() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/orders");
        req.setRemoteAddr("10.0.0.1");
        return req;
    }

    private FilterChain passingChain(boolean[] called) {
        return (req, res) -> called[0] = true;
    }

    @Test
    void allowsUpToThirtyPerMinute() throws Exception {
        boolean[] called = { false };
        for (int i = 0; i < 30; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(postOrder(), res, passingChain(called));
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
        assertThat(called[0]).isTrue();
    }

    @Test
    void thirtyFirstIsRejected() throws Exception {
        boolean[] called = { false };
        for (int i = 0; i < 30; i++) {
            filter.doFilter(postOrder(), new MockHttpServletResponse(), passingChain(called));
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(postOrder(), res, passingChain(called));
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getContentAsString()).contains("rate limit");
    }

    @Test
    void otherPathsUntouched() throws Exception {
        boolean[] called = { false };
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/orders");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, passingChain(called));
        assertThat(called[0]).isTrue();
        assertThat(res.getStatus()).isNotEqualTo(429);
    }
}
