package com.omnishop.order.service;

import com.omnishop.order.dto.*;
import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO request);
    OrderResponseDTO getOrderById(Long id);
    List<OrderResponseDTO> getOrdersByUser(Long userId);
    List<OrderResponseDTO> getAllOrders(String status);
    List<OrderResponseDTO> getHighValueOrders(java.math.BigDecimal minTotal);
    OrderResponseDTO updateStatus(Long id, String status);
    OrderResponseDTO cancelOrder(Long id);
}
