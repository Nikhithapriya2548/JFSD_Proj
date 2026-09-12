package com.omnishop.order.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private final RestClient paymentRestClient;

    /**
     * Calls the SIMULATED payment gateway. Returns "SUCCESS"/"FAILED".
     * Throws on transport failure so the caller can keep the order PENDING
     * (Wave 2 formalizes this with Resilience4j circuit breaking).
     */
    public String charge(Long orderId, java.math.BigDecimal amount, String method) {
        log.info("Charging order {} amount {} via payment-service", orderId, amount);
        PaymentResponse res = paymentRestClient.post()
                .uri("/api/v1/payments")
                .header("X-Correlation-ID", MDC.get("correlationId"))
                .body(new PaymentRequest(orderId, amount, method))
                .retrieve()
                .body(PaymentResponse.class);
        return res == null ? "FAILED" : res.status();
    }

    public record PaymentRequest(Long orderId, java.math.BigDecimal amount, String method) {}
    public record PaymentResponse(String status) {}
}
