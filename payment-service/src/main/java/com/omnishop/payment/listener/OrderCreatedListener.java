package com.omnishop.payment.listener;

import com.omnishop.payment.dto.*;
import com.omnishop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

import static com.omnishop.payment.config.RabbitConfig.EXCHANGE;

/**
 * SAGA STEP 2: consumes order.created, runs the SIMULATED gateway, and
 * publishes the verdict. order-service reacts; this service never touches
 * order state directly — each service owns its own data.
 */
@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
    private final PaymentService payments;
    private final RabbitTemplate rabbit;

    @RabbitListener(queues = "payment.order.created")
    public void onOrderCreated(@Payload Map<String, Object> event) {
        Object cid = event.get("correlationId");
        if (cid != null) MDC.put("correlationId", cid.toString());
        try {
            handleOrderCreated(event, cid == null ? null : cid.toString());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void handleOrderCreated(Map<String, Object> event, String correlationId) {
        Long orderId = toLong(event.get("orderId"));
        Long userId = toLong(event.get("userId"));
        if (orderId == null) {
            log.warn("order.created without orderId, ignoring: {}", event);
            return;
        }
        BigDecimal amount = new BigDecimal(String.valueOf(event.getOrDefault("totalAmount", "0")));
        String method = String.valueOf(event.getOrDefault("paymentMethod", "CARD"));
        log.info("Saga step: processing mock payment for order {} amount {}", orderId, amount);

        PaymentResponse res = payments.process(
                PaymentRequest.builder().orderId(orderId).amount(amount).method(method).build());

        boolean ok = "SUCCESS".equalsIgnoreCase(res.getStatus());
        Map<String, Object> verdict = new HashMap<>();
        verdict.put("eventType", ok ? "payment.completed" : "payment.failed");
        verdict.put("orderId", orderId);
        verdict.put("userId", userId);
        verdict.put("amount", amount);
        verdict.put("transactionId", res.getTransactionId());
        if (correlationId != null) verdict.put("correlationId", correlationId);
        if (!ok) verdict.put("reason", "Mock gateway declined the transaction");
        rabbit.convertAndSend(EXCHANGE, (String) verdict.get("eventType"), verdict);
        log.info("Saga step: published {} for order {}", verdict.get("eventType"), orderId);
    }

    private Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o == null ? null : Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
