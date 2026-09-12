package com.omnishop.order.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal value;  // percent (e.g. 10) or flat amount

    private BigDecimal minOrderAmount;
    private LocalDateTime expiryDate;
    private Integer usageLimit;
    private Integer timesUsed = 0;

    public enum DiscountType { PERCENTAGE, FIXED }
}
