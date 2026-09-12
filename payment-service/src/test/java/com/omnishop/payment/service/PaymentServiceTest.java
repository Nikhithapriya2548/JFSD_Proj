package com.omnishop.payment.service;

import com.omnishop.payment.dto.*;
import com.omnishop.payment.model.Payment;
import com.omnishop.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository repository;
    @InjectMocks PaymentService service;

    private void gateway(double successRate) {
        ReflectionTestUtils.setField(service, "simulatedDelayMs", 0L);
        ReflectionTestUtils.setField(service, "successRate", successRate);
        // lenient: the unsupported-method test throws before reaching save
        lenient().when(repository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PaymentRequest request() {
        PaymentRequest req = new PaymentRequest();
        req.setOrderId(1L);
        req.setAmount(new BigDecimal("250.00"));
        req.setMethod("UPI");
        return req;
    }

    @Test
    void certainGatewayAlwaysSucceeds() {
        gateway(1.0);
        PaymentResponse res = service.process(request());
        assertThat(res.getStatus()).isEqualTo("SUCCESS");
        assertThat(res.getMethod()).isEqualTo("UPI");
        assertThat(res.getTransactionId()).startsWith("MOCK-TXN-");
    }

    @Test
    void deadGatewayAlwaysFails() {
        gateway(0.0);
        PaymentResponse res = service.process(request());
        assertThat(res.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void unsupportedMethodRejected() {
        gateway(1.0);
        PaymentRequest req = request();
        req.setMethod("BITCOIN");
        assertThatThrownBy(() -> service.process(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported payment method");
    }

    @Test
    void receiptReturnsAttempt() {
        Payment p = Payment.builder().id(9L).orderId(1L)
                .amount(new BigDecimal("250.00")).method(Payment.PaymentMethod.UPI)
                .status(Payment.PaymentStatus.SUCCESS).transactionId("MOCK-TXN-ABC123").build();
        when(repository.findById(9L)).thenReturn(Optional.of(p));
        var res = service.receipt(9L);
        assertThat(res.getTransactionId()).isEqualTo("MOCK-TXN-ABC123");
        assertThat(res.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void receiptMissingIs404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.receipt(99L))
                .isInstanceOf(com.omnishop.payment.exception.PaymentNotFoundException.class);
    }
}
