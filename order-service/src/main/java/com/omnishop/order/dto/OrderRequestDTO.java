package com.omnishop.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderRequestDTO {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotEmpty(message = "At least one item is required")
    private List<OrderItemRequest> items;

    // CARD, UPI, COD — forwarded to payment-service (default CARD)
    private String paymentMethod;

    // Optional discount code, validated + consumed at order creation
    private String couponCode;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "productId is required")
        private Long productId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;
    }
}
