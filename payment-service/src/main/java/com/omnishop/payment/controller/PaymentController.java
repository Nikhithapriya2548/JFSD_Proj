package com.omnishop.payment.controller;

import com.omnishop.payment.dto.*;
import com.omnishop.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "SIMULATED gateway (demo only — no real money moves)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Process payment (simulated)",
            description = "Mock gateway: fixed delay, ~85% success. "
                    + "Example: {\"orderId\":1,\"amount\":9999.00,\"method\":\"UPI\"}. "
                    + "method is CARD, UPI or COD (default CARD).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment recorded (SUCCESS or FAILED)"),
        @ApiResponse(responseCode = "400", description = "Validation failed / bad method")
    })
    public PaymentResponse process(@Valid @RequestBody PaymentRequest req) {
        return paymentService.process(req);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Payments for an order")
    @ApiResponse(responseCode = "200", description = "Payment list")
    public List<PaymentResponse> byOrder(@PathVariable Long orderId) {
        return paymentService.byOrder(orderId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Payment receipt",
            description = "Single payment attempt with transaction id, method, amount and status — "
                    + "the receipt view backing the order-history retry flow.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment receipt"),
        @ApiResponse(responseCode = "404", description = "Unknown payment id")
    })
    public PaymentResponse receipt(@PathVariable Long id) {
        return paymentService.receipt(id);
    }
}
