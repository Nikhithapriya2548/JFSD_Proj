package com.omnishop.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentRequest {
    @NotNull private Long orderId;
    @NotNull @Positive private BigDecimal amount;
    // CARD, UPI, COD — defaults to CARD when omitted
    private String method;
}
