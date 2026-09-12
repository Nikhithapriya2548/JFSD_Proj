package com.omnishop.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private Long userId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private List<ItemDTO> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemDTO {
        private Long productId;
        private Integer quantity;
        private BigDecimal priceAtPurchase;
    }
}
