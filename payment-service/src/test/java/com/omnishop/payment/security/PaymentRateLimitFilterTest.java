package com.omnishop.payment.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class PaymentRateLimitFilterTest {

    private final PaymentRateLimitFilter filter = new PaymentRateLimitFilter();

    private MockHttpServletRequest postPayment() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/payments");
        req.setRemoteAddr("10.0.0.9");
        return req;
    }

    @Test
    void thirtyFirstPostIsRejected() throws Exception {
        boolean[] called = { false };
        FilterChain chain = (req, res) -> called[0] = true;
        for (int i = 0; i < 30; i++) {
            filter.doFilter(postPayment(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(postPayment(), res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void getReceiptNeverLimited() throws Exception {
        boolean[] called = { false };
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/payments/1");
        req.setRemoteAddr("10.0.0.9");
        filter.doFilter(req, new MockHttpServletResponse(), (rq, rs) -> called[0] = true);
        assertThat(called[0]).isTrue();
    }
}
