package com.omnishop.notification.listener;

import com.omnishop.notification.model.Notification;
import com.omnishop.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Pure event consumer — no REST calls in, only events.
 * Each event becomes a persisted "notification sent" record holding the
 * would-be email/SMS text. Nothing is actually sent anywhere
 * (no real provider; same reasoning as the mock payment gateway).
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final NotificationRepository repository;

    @RabbitListener(queues = "notify.events")
    public void onEvent(@Payload Map<String, Object> event) {
        Object cid = event.get("correlationId");
        if (cid != null) MDC.put("correlationId", cid.toString());
        try {
            handleEvent(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void handleEvent(Map<String, Object> event) {
        String type = (String) event.getOrDefault("eventType", "unknown");
        Long userId = toLong(event.get("userId"));
        Long refId = toLong(event.getOrDefault("orderId", event.get("productId")));
        String message = render(type, event);

        repository.save(Notification.builder()
                .userId(userId).eventType(type).refId(refId).message(message).build());
        // Simulated send — the message content is the artifact
        log.info("[notification sent] type={} user={} ref={} :: {}", type, userId, refId, message);
    }

    private String render(String type, Map<String, Object> e) {
        return switch (type) {
            case "order.created" -> String.format(
                    "Hi! Your order #%s for $%s is received and payment is processing.",
                    e.get("orderId"), e.get("totalAmount"));
            case "order.status.changed" -> String.format(
                    "Order #%s update: %s → %s.", e.get("orderId"), e.get("oldStatus"), e.get("newStatus"));
            case "payment.completed" -> String.format(
                    "Payment of $%s for order #%s succeeded (txn %s).",
                    e.get("amount"), e.get("orderId"), e.get("transactionId"));
            case "payment.failed" -> String.format(
                    "Payment for order #%s could not be completed. Your order is kept on hold — please retry.",
                    e.get("orderId"));
            case "order.payment_failed" -> String.format(
                    "Heads up: payment failed for order #%s, reserved stock was released back.",
                    e.get("orderId"));
            case "stock.low" -> String.format(
                    "Admin alert: '%s' (id %s) is low on stock: %s left.",
                    e.get("name"), e.get("productId"), e.get("stockQuantity"));
            default -> "Event received: " + e;
        };
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
