package com.omnishop.order.client;

import com.omnishop.order.dto.ProductResponseDTO;
import com.omnishop.order.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final RestClient productRestClient;

    public ProductResponseDTO getProduct(Long productId) {
        log.info("Fetching product {} from product-service", productId);
        return productRestClient.get()
                .uri("/api/v1/products/{id}", productId)
                .header("X-Correlation-ID", MDC.get("correlationId"))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        (request, response) -> { throw new ProductNotFoundException(productId); })
                .body(ProductResponseDTO.class);
    }
}
