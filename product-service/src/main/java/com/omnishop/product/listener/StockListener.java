package com.omnishop.product.listener;

import com.omnishop.product.model.Product;
import com.omnishop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import static com.omnishop.product.config.AppConfig.EXCHANGE;

/**
 * SAGA stock participant: reserves stock when an order starts, restores it
 * if payment fails (compensating transaction). Evicts the product cache on
 * every change so reads never serve stale stock counts.
 */
@Component
@RequiredArgsConstructor
public class StockListener {

    private static final Logger log = LoggerFactory.getLogger(StockListener.class);
    private final ProductRepository repository;
    private final RabbitTemplate rabbit;

    @Value("${app.stock.low-threshold:10}")
    private int lowThreshold;

    // Join the saga trace carried inside the event (no HTTP headers on queues)
    private void joinTrace(Map<String, Object> event) {
        Object cid = event.get("correlationId");
        if (cid != null) MDC.put("correlationId", cid.toString());
    }

    @RabbitListener(queues = "product.order.created")
    @Transactional
    @CacheEvict(value = {"products", "productList"}, allEntries = true)
    public void onOrderCreated(@Payload Map<String, Object> event) {
        joinTrace(event);
        try {
            reserveStock(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void reserveStock(Map<String, Object> event) {
        List<Map<String, Object>> items = itemsOf(event.get("items"));
        for (Map<String, Object> item : items) {
            Long productId = toLong(item.get("productId"));
            int qty = toInt(item.get("quantity"));
            if (productId == null || qty <= 0) continue;
            try {
                repository.findById(productId).ifPresentOrElse(p -> {
                    // Optimistic reserve: never go below zero even under races
                    p.setStockQuantity(Math.max(0, p.getStockQuantity() - qty));
                    repository.save(p);
                    log.info("Stock reserved: product={} -{} remaining={}",
                            productId, qty, p.getStockQuantity());
                    if (p.getStockQuantity() < lowThreshold) {
                        rabbit.convertAndSend(EXCHANGE, "stock.low", Map.of(
                                "eventType", "stock.low",
                                "productId", p.getId(),
                                "name", p.getName(),
                                "stockQuantity", p.getStockQuantity()));
                    }
                }, () -> log.warn("Reserve for unknown product {}, ignoring", productId));
            } catch (ObjectOptimisticLockingFailureException e) {
                // Concurrent decrement collided on @Version — the event will
                // be redelivered; log and let the retry converge the count.
                log.warn("Optimistic lock collision reserving product {}, will converge on retry", productId);
                throw e;
            }
        }
    }

    @RabbitListener(queues = "product.order.payment_failed")
    @Transactional
    @CacheEvict(value = {"products", "productList"}, allEntries = true)
    public void onPaymentFailed(@Payload Map<String, Object> event) {
        joinTrace(event);
        try {
            restoreStock(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void restoreStock(Map<String, Object> event) {
        // Compensating transaction: give the reserved stock back
        List<Map<String, Object>> items = itemsOf(event.get("items"));
        for (Map<String, Object> item : items) {
            Long productId = toLong(item.get("productId"));
            int qty = toInt(item.get("quantity"));
            if (productId == null || qty <= 0) continue;
            repository.findById(productId).ifPresent(p -> {
                p.setStockQuantity(p.getStockQuantity() + qty);
                repository.save(p);
                log.info("Stock restored (compensation): product={} +{} now={}",
                        productId, qty, p.getStockQuantity());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemsOf(Object o) {
        if (o instanceof List<?> l) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object e : l) if (e instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
            return out;
        }
        return List.of();
    }

    private Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o == null ? null : Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int toInt(Object o) {
        Long l = toLong(o);
        return l == null ? 0 : l.intValue();
    }
}
