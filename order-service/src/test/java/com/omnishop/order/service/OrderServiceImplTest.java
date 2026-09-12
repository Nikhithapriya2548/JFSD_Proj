package com.omnishop.order.service;

import com.omnishop.order.client.PaymentClient;
import com.omnishop.order.client.ProductClient;
import com.omnishop.order.dto.*;
import com.omnishop.order.events.OrderEventPublisher;
import com.omnishop.order.exception.InsufficientStockException;
import com.omnishop.order.model.Order;
import com.omnishop.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository repository;
    @Mock ProductClient productClient;
    @Mock PaymentClient paymentClient;
    @Mock CouponService couponService;
    @Mock OrderEventPublisher events;
    @InjectMocks OrderServiceImpl service;

    private ProductResponseDTO product(BigDecimal price, int stock) {
        ProductResponseDTO p = new ProductResponseDTO();
        p.setId(1L);
        p.setPrice(price);
        p.setStockQuantity(stock);
        return p;
    }

    private OrderRequestDTO request(Long productId, int qty, String coupon) {
        OrderRequestDTO.OrderItemRequest item = new OrderRequestDTO.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(qty);
        OrderRequestDTO req = new OrderRequestDTO();
        req.setUserId(7L);
        req.setItems(List.of(item));
        req.setPaymentMethod("CARD");
        req.setCouponCode(coupon);
        return req;
    }

    private Order order(Order.OrderStatus status, String total) {
        Order o = new Order();
        o.setId(1L);
        o.setUserId(7L);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal(total));
        o.setOrderDate(LocalDateTime.now());
        o.setItems(new ArrayList<>());
        return o;
    }

    @Test
    void createComputesTotalAndStartsSaga() {
        when(productClient.getProduct(1L)).thenReturn(product(new BigDecimal("100.00"), 10));
        when(repository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrderResponseDTO res = service.createOrder(request(1L, 2, null));

        assertThat(res.getStatus()).isEqualTo("PENDING");
        assertThat(res.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(events).orderCreated(eq(1L), eq(7L), eq(new BigDecimal("200.00")), anyList(), eq("CARD"));
    }

    @Test
    void createAppliesCouponDiscount() {
        when(productClient.getProduct(1L)).thenReturn(product(new BigDecimal("200.00"), 5));
        when(couponService.consume(eq("SAVE10"), eq(new BigDecimal("200.00"))))
                .thenReturn(new CouponService.Validation(new BigDecimal("20.00"), "OK"));
        when(repository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(2L);
            return o;
        });

        OrderResponseDTO res = service.createOrder(request(1L, 1, "SAVE10"));

        assertThat(res.getTotalAmount()).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    void createRejectsInsufficientStock() {
        when(productClient.getProduct(1L)).thenReturn(product(new BigDecimal("50.00"), 1));

        assertThatThrownBy(() -> service.createOrder(request(1L, 9999, null)))
                .isInstanceOf(InsufficientStockException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getByIdMissingIs404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOrderById(9L))
                .isInstanceOf(OrderServiceImpl.OrderNotFoundException.class);
    }

    @Test
    void cancelPendingOrder() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(Order.OrderStatus.PENDING, "100.00")));
        when(repository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponseDTO res = service.cancelOrder(1L);

        assertThat(res.getStatus()).isEqualTo("CANCELLED");
        verify(events).statusChanged(1L, 7L, "PENDING", "CANCELLED");
    }

    @Test
    void cancelNonPendingRejected() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(Order.OrderStatus.CONFIRMED, "100.00")));

        assertThatThrownBy(() -> service.cancelOrder(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");
    }

    @Test
    void updateStatusAppliesAndPublishes() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(Order.OrderStatus.CONFIRMED, "100.00")));
        when(repository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponseDTO res = service.updateStatus(1L, "SHIPPED");

        assertThat(res.getStatus()).isEqualTo("SHIPPED");
        verify(events).statusChanged(1L, 7L, "CONFIRMED", "SHIPPED");
    }

    @Test
    void updateStatusRejectsBogus() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(Order.OrderStatus.CONFIRMED, "100.00")));

        assertThatThrownBy(() -> service.updateStatus(1L, "BOGUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }
}
