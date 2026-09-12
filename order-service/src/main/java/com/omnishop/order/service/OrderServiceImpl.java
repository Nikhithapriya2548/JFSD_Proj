package com.omnishop.order.service;

import com.omnishop.order.client.ProductClient;
import com.omnishop.order.client.PaymentClient;
import com.omnishop.order.events.OrderEventPublisher;
import com.omnishop.order.dto.*;
import com.omnishop.order.exception.InsufficientStockException;
import com.omnishop.order.model.*;
import com.omnishop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository repository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final CouponService couponService;
    private final OrderEventPublisher events;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        Order order = Order.builder()
                .userId(request.getUserId())
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .build();

        // a–d. Live price/stock lookup per item; snapshot price, never trust client
        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> {
                    ProductResponseDTO product = productClient.getProduct(itemReq.getProductId());
                    int available = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                    if (available < itemReq.getQuantity()) {
                        log.warn("Insufficient stock rejected: product={} available={} requested={}",
                                itemReq.getProductId(), available, itemReq.getQuantity());
                        throw new InsufficientStockException(
                                itemReq.getProductId(), available, itemReq.getQuantity());
                    }
                    return OrderItem.builder()
                            .order(order)
                            .productId(itemReq.getProductId())
                            .quantity(itemReq.getQuantity())
                            .priceAtPurchase(product.getPrice())
                            .build();
                })
                .collect(Collectors.toList());
        order.setItems(items);

        // e. Streams-only subtotal — no for-loop
        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Coupon: validate + consume, then discount the Streams-computed subtotal
        BigDecimal total = subtotal;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            CouponService.Validation v = couponService.consume(couponCode.trim(), subtotal);
            total = subtotal.subtract(v.discount());
            log.info("Coupon applied: {} discount={} subtotal={} total={}",
                    couponCode, v.discount(), subtotal, total);
        }
        order.setTotalAmount(total);

        // f. SAGA STEP 1: persist PENDING, publish order.created, return
        // immediately. Payment + stock happen asynchronously; the verdict
        // arrives later via payment.completed / payment.failed events.
        // Replaces the old synchronous payment-service REST call.
        Order saved = repository.save(order);
        List<Map<String, Object>> eventItems = saved.getItems().stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", i.getProductId());
                    m.put("quantity", i.getQuantity());
                    return m;
                })
                .collect(Collectors.toList());
        String method = request.getPaymentMethod() == null ? "CARD" : request.getPaymentMethod();
        events.orderCreated(saved.getId(), saved.getUserId(), saved.getTotalAmount(), eventItems, method);
        OrderResponseDTO result = toDTO(saved);
        log.info("Order placed (PENDING, saga started): id={} user={} items={} total={}",
                result.getId(), result.getUserId(), result.getItems().size(), result.getTotalAmount());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        return toDTO(repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUser(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders(String status) {
        return repository.findAll().stream()
                .filter(o -> status == null || status.isBlank()
                        || o.getStatus().name().equalsIgnoreCase(status))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getHighValueOrders(java.math.BigDecimal minTotal) {
        return repository.findAll().stream()
                .filter(o -> o.getTotalAmount() != null && o.getTotalAmount().compareTo(minTotal) > 0)
                .sorted(java.util.Comparator.comparing(Order::getTotalAmount).reversed())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDTO updateStatus(Long id, String status) {
        // NOTE: JWT role check (admin-only) lands in Wave 2 — endpoint first.
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        Order.OrderStatus next;
        try {
            next = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid status: " + status
                    + " (use PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, PAYMENT_FAILED)");
        }
        Order.OrderStatus old = order.getStatus();
        order.setStatus(next);
        OrderResponseDTO result = toDTO(repository.save(order));
        log.info("Order status updated: id={} -> {}", id, next);
        events.statusChanged(id, order.getUserId(), old.name(), next.name());
        return result;
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        OrderResponseDTO result = toDTO(repository.save(order));
        log.info("Order cancelled: id={}", result.getId());
        events.statusChanged(id, order.getUserId(), "PENDING", "CANCELLED");
        return result;
    }

    private OrderResponseDTO toDTO(Order o) {
        return OrderResponseDTO.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .orderDate(o.getOrderDate())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .items(o.getItems().stream()
                        .map(i -> OrderResponseDTO.ItemDTO.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .priceAtPurchase(i.getPriceAtPurchase())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(Long id) { super("Order not found: " + id); }
    }
}
