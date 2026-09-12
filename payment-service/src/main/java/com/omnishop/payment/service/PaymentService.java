package com.omnishop.payment.service;

import com.omnishop.payment.dto.*;
import com.omnishop.payment.exception.PaymentNotFoundException;
import com.omnishop.payment.model.Payment;
import com.omnishop.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SIMULATED payment gateway — for demonstration purposes ONLY.
 * No real money moves here: outcomes are random with a fixed delay,
 * standing in for a gateway like Razorpay/Stripe. Real payment processing
 * (PCI-DSS, webhooks, refunds) is out of scope for this student project.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repository;

    // Simulated gateway latency (ms) and success probability — overridable via env
    @Value("${payment.simulated-delay-ms:1500}")
    private long simulatedDelayMs;

    @Value("${payment.success-rate:0.85}")
    private double successRate;

    @Transactional
    public PaymentResponse process(PaymentRequest req) {
        Payment.PaymentMethod method;
        try {
            method = req.getMethod() == null ? Payment.PaymentMethod.CARD
                    : Payment.PaymentMethod.valueOf(req.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported payment method: " + req.getMethod()
                    + " (use CARD, UPI or COD)");
        }

        // Simulate gateway latency
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate gateway outcome
        boolean ok = ThreadLocalRandom.current().nextDouble() < successRate;
        Payment payment = Payment.builder()
                .orderId(req.getOrderId())
                .amount(req.getAmount())
                .method(method)
                .status(ok ? Payment.PaymentStatus.SUCCESS : Payment.PaymentStatus.FAILED)
                .transactionId("MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        Payment saved = repository.save(payment);
        log.info("Mock payment {}: order={} amount={} method={} txn={}",
                saved.getStatus(), saved.getOrderId(), saved.getAmount(),
                saved.getMethod(), saved.getTransactionId());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> byOrder(Long orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse receipt(Long id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private PaymentResponse toDTO(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).orderId(p.getOrderId()).amount(p.getAmount())
                .status(p.getStatus().name()).method(p.getMethod().name())
                .transactionId(p.getTransactionId()).createdAt(p.getCreatedAt()).build();
    }
}
