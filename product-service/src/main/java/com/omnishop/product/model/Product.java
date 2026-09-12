package com.omnishop.product.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    private String category;

    // Nullable: listings without a usable image store null (placeholder in UI).
    // TEXT-length: Unsplash URLs with signed params exceed varchar(255).
    @Column(length = 2048)
    private String imageUrl;

    // Optimistic locking: concurrent order.created consumers decrementing
    // the same product's stock collide here instead of silently overwriting.
    @Version
    private Long version;

    private LocalDateTime createdAt = LocalDateTime.now();
}
