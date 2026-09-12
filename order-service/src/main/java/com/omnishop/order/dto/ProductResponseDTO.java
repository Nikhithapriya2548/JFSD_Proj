package com.omnishop.order.dto;

import lombok.*;
import java.math.BigDecimal;

// Mirrors product-service's ProductResponseDTO for deserialization
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
}
