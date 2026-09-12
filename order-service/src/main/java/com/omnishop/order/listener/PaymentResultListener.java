package com.omnishop.order.listener;

import com.omnishop.order.events.OrderEventPublisher;
import com.omnishop.order.model.Order;
import com.omnishop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Saga participant, order side: reacts to the SIMULATED gateway's verdict.
 * - payment.completed → CONFIRMED (+ status event)
 * - payment.failed   → PAYMENT_FAILED (+ status event + compensation event
 *   so product-service can restore the reserved stock)
 * The DB row is updated idempotently: duplicate deliveries of the same
 * verdict converge on the same state.
 */
@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);
    private final OrderRepository repository;
    private final OrderEventPublisher events;

    @RabbitListener(queues = "order.payment.results")
    @Transactional
    public void onPaymentResult(@Payload Map<String, Object> event) {
        // Join the saga trace: RabbitMQ threads have no HTTP filter upstream
        String cid = event.get("correlationId") == null ? null : event.get("correlationId").toString();
        if (cid != null) MDC.put("correlationId", cid);
        try {
            handlePaymentResult(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void handlePaymentResult(Map<String, Object> event) {
        Long orderId = toLong(event.get("orderId"));
        if (orderId == null) {
            log.warn("Payment result without orderId, ignoring: {}", event);
            return;
        }
        Optional<Order> maybe = repository.findById(orderId);
        if (maybe.isEmpty()) {
            log.warn("Payment result for unknown order {}, ignoring", orderId);
            return;
        }
        Order order = maybe.get();
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            log.info("Duplicate/late payment verdict for order {} (already {}), ignoring",
                    orderId, order.getStatus());
            return;
        }
        String eventType = String.valueOf(event.getOrDefault("eventType", ""));
        if (eventType.contains("completed")) {
            transition(order, Order.OrderStatus.CONFIRMED);
        } else {
            transition(order, Order.OrderStatus.PAYMENT_FAILED);
            events.paymentFailedCompensation(order.getId(), order.getUserId(), itemsOf(order));
        }
    }

    private void transition(Order order, Order.OrderStatus next) {
        Order.OrderStatus old = order.getStatus();
        order.setStatus(next);
        repository.save(order);
        log.info("Saga step: order {} {} -> {}", order.getId(), old, next);
        events.statusChanged(order.getId(), order.getUserId(), old.name(), next.name());
    }

    private List<Map<String, Object>> itemsOf(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (var i : order.getItems()) {
            items.add(Map.of("productId", i.getProductId(), "quantity", i.getQuantity()));
        }
        return items;
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
