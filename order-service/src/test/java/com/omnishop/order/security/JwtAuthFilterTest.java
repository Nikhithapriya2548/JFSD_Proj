package com.omnishop.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtAuthFilterTest {

    private static final String SECRET = "order-test-secret-key-min-32-chars!!";
    private JwtAuthFilter filter;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String token(String sub, String role, long ttlMs) {
        return Jwts.builder().subject(sub).claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key).compact();
    }

    private MockHttpServletRequest requestWith(String header) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (header != null) req.addHeader("Authorization", header);
        return req;
    }

    @Test
    void validTokenSetsAdminAuthority() throws Exception {
        filter.doFilter(requestWith("Bearer " + token("3", "ADMIN", 60000)),
                new MockHttpServletResponse(), mockChain());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("3");
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void tamperedTokenStaysAnonymous() throws Exception {
        filter.doFilter(requestWith("Bearer " + token("3", "ADMIN", 60000) + "x"),
                new MockHttpServletResponse(), mockChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingHeaderStaysAnonymous() throws Exception {
        filter.doFilter(requestWith(null), new MockHttpServletResponse(), mockChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredTokenStaysAnonymous() throws Exception {
        filter.doFilter(requestWith("Bearer " + token("3", "CUSTOMER", -1000)),
                new MockHttpServletResponse(), mockChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private FilterChain mockChain() {
        return (req, res) -> {};
    }
}
