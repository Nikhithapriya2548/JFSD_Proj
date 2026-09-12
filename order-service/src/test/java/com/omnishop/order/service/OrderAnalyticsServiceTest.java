package com.omnishop.order.service;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAnalyticsServiceTest {

    @Mock OrderRepository repository;
    @InjectMocks OrderAnalyticsService service;

    private Order order(Order.OrderStatus status, String amount, int daysAgo) {
        Order o = new Order();
        o.setUserId(7L);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal(amount));
        o.setOrderDate(LocalDateTime.now().minusDays(daysAgo));
        return o;
    }

    @Test
    void groupsRevenueByDayAndExcludesCancelled() {
        String today = LocalDateTime.now().toLocalDate().toString();
        when(repository.findAll()).thenReturn(List.of(
                order(Order.OrderStatus.DELIVERED, "100.00", 0),
                order(Order.OrderStatus.SHIPPED, "50.00", 0),
                order(Order.OrderStatus.CANCELLED, "999.00", 0),
                order(Order.OrderStatus.PAYMENT_FAILED, "999.00", 0)));

        Map<String, Object> out = service.summary();

        assertThat(out.get("totalOrders")).isEqualTo(4);
        assertThat((BigDecimal) out.get("revenueTotal")).isEqualByComparingTo(new BigDecimal("150.00"));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byDay = (Map<String, BigDecimal>) out.get("revenueByDay");
        assertThat(byDay.get(today)).isEqualByComparingTo(new BigDecimal("150.00"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) out.get("statusDistribution");
        assertThat(byStatus.get("CANCELLED")).isEqualTo(1L);
    }

    @Test
    void ignoresOrdersOlderThan30Days() {
        when(repository.findAll()).thenReturn(List.of(
                order(Order.OrderStatus.DELIVERED, "100.00", 60)));
        Map<String, Object> out = service.summary();
        assertThat(out.get("totalOrders")).isEqualTo(1);
        assertThat((BigDecimal) out.get("revenueTotal")).isEqualByComparingTo(BigDecimal.ZERO);
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byDay = (Map<String, BigDecimal>) out.get("revenueByDay");
        assertThat(byDay).isEmpty();
    }
}
