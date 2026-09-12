package com.omnishop.order.events;

import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

import static com.omnishop.order.config.RabbitConfig.EXCHANGE;

/**
 * Publishes order lifecycle events. Fire-and-forget: if the broker is down
 * the order stays PENDING in the DB and the flow is retried later —
 * the DB row is the source of truth, never the message.
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final RabbitTemplate rabbit;

    public void orderCreated(Long orderId, Long userId, java.math.BigDecimal total,
                             List<Map<String, Object>> items, String paymentMethod) {
        publish("order.created", withCorrelation(Map.of(
                "eventType", "order.created",
                "orderId", orderId, "userId", userId,
                "totalAmount", total, "items", items,
                "paymentMethod", paymentMethod == null ? "CARD" : paymentMethod)));
    }

    public void statusChanged(Long orderId, Long userId, String oldStatus, String newStatus) {
        publish("order.status.changed", withCorrelation(Map.of(
                "eventType", "order.status.changed",
                "orderId", orderId, "userId", userId,
                "oldStatus", oldStatus, "newStatus", newStatus)));
    }

    public void paymentFailedCompensation(Long orderId, Long userId, List<Map<String, Object>> items) {
        publish("order.payment_failed", withCorrelation(Map.of(
                "eventType", "order.payment_failed",
                "orderId", orderId, "userId", userId, "items", items)));
    }

    // The correlation ID rides inside the event so every downstream consumer
    // can join the same trace even though messaging has no HTTP headers here.
    private Map<String, Object> withCorrelation(Map<String, Object> payload) {
        String cid = MDC.get("correlationId");
        if (cid == null) return payload;
        Map<String, Object> m = new HashMap<>(payload);
        m.put("correlationId", cid);
        return m;
    }

    private void publish(String key, Map<String, Object> payload) {
        try {
            rabbit.convertAndSend(EXCHANGE, key, payload);
            log.info("Event published: {} order={}", key, payload.get("orderId"));
        } catch (Exception e) {
            log.warn("Broker unreachable, event {} kept as DB state only: {}", key, e.getMessage());
        }
    }
}
