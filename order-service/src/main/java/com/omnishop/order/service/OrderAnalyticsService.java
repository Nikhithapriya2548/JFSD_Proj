package com.omnishop.order.service;

import com.omnishop.order.model.Order;
import com.omnishop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAnalyticsService {

    private final OrderRepository repository;

    /**
     * Admin overview computed with Streams grouping: revenue per day for the
     * last 30 days, status distribution, totals. Only successful orders
     * (not CANCELLED / PAYMENT_FAILED) count toward revenue.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<Order> orders = repository.findAll();
        LocalDate cutoff = LocalDate.now().minusDays(30);

        Map<String, BigDecimal> revenueByDay = orders.stream()
                .filter(o -> o.getTotalAmount() != null)
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED
                        && o.getStatus() != Order.OrderStatus.PAYMENT_FAILED)
                .filter(o -> o.getOrderDate() != null
                        && !o.getOrderDate().toLocalDate().isBefore(cutoff))
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                Order::getTotalAmount, BigDecimal::add)));

        Map<String, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(), Collectors.counting()));

        BigDecimal revenue = revenueByDay.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalOrders", orders.size());
        out.put("revenueTotal", revenue);
        out.put("revenueByDay", revenueByDay);
        out.put("statusDistribution", byStatus);
        return out;
    }
}
