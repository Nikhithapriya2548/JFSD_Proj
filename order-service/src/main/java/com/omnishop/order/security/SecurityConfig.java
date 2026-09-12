package com.omnishop.order.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.Instant;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    private static void deny(HttpServletResponse res, int status, String error, String message, String path)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status, error, message, path));
    }

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) ->
                            deny(res, 401, "Unauthorized", "Authentication required", req.getRequestURI()))
                    .accessDeniedHandler((req, res, ex) ->
                            deny(res, 403, "Forbidden", "Admin role required", req.getRequestURI())))
            .headers(h -> h
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(f -> f.deny()))
            .authorizeHttpRequests(a -> a
                    // Admin-only operations
                    .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/status").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/orders/analytics/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/coupons").hasRole("ADMIN")
                    // Order + coupon reads/writes need any login
                    .requestMatchers("/api/v1/orders/**").authenticated()
                    .requestMatchers("/api/v1/coupons/**").authenticated()
                    // Ops/docs always open
                    .requestMatchers("/actuator/health", "/actuator/info",
                            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
