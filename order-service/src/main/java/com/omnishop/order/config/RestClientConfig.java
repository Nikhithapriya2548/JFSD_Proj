package com.omnishop.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(
            RestClient.Builder builder,
            @Value("${product-service.url:http://localhost:8081}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean
    public RestClient paymentRestClient(
            RestClient.Builder builder,
            @Value("${payment-service.url:http://localhost:8084}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        // Mock gateway sleeps ~1.5s; allow headroom
        factory.setReadTimeout(Duration.ofSeconds(10));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
