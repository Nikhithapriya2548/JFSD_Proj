package com.omnishop.order.service;

import com.omnishop.order.model.Coupon;
import com.omnishop.order.repository.CouponRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock CouponRepository repository;
    @InjectMocks CouponService service;

    private Coupon coupon(String code) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        c.setValue(new BigDecimal("10"));
        c.setTimesUsed(0);
        return c;
    }

    private void stub(Coupon c) {
        lenient().when(repository.findByCodeIgnoreCase(c.getCode())).thenReturn(Optional.of(c));
    }

    @Test
    void percentageDiscountMath() {
        Coupon c = coupon("SAVE10");
        stub(c);
        CouponService.Validation v = service.validate("SAVE10", new BigDecimal("200.00"));
        assertThat(v.discount()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void fixedDiscountCappedAtSubtotal() {
        Coupon c = coupon("FLAT50");
        c.setDiscountType(Coupon.DiscountType.FIXED);
        c.setValue(new BigDecimal("50"));
        stub(c);
        assertThat(service.validate("FLAT50", new BigDecimal("30")).discount())
                .isEqualByComparingTo(new BigDecimal("30"));
    }

    @Test
    void unknownCodeRejected() {
        when(repository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validate("NOPE", BigDecimal.TEN))
                .isInstanceOf(CouponService.InvalidCouponException.class)
                .hasMessageContaining("Unknown coupon");
    }

    @Test
    void expiredRejected() {
        Coupon c = coupon("OLD");
        c.setExpiryDate(LocalDateTime.now().minusDays(1));
        stub(c);
        assertThatThrownBy(() -> service.validate("OLD", new BigDecimal("500")))
                .isInstanceOf(CouponService.InvalidCouponException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void usageLimitEnforced() {
        Coupon c = coupon("ONCE");
        c.setUsageLimit(1);
        c.setTimesUsed(1);
        stub(c);
        assertThatThrownBy(() -> service.validate("ONCE", new BigDecimal("500")))
                .isInstanceOf(CouponService.InvalidCouponException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    void minimumOrderEnforced() {
        Coupon c = coupon("BIG");
        c.setMinOrderAmount(new BigDecimal("1000"));
        stub(c);
        assertThatThrownBy(() -> service.validate("BIG", new BigDecimal("100")))
                .isInstanceOf(CouponService.InvalidCouponException.class)
                .hasMessageContaining("minimum order");
    }

    @Test
    void consumeIncrementsUsage() {
        Coupon c = coupon("SAVE10");
        stub(c);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.consume("SAVE10", new BigDecimal("200"));
        assertThat(c.getTimesUsed()).isEqualTo(1);
        verify(repository).save(c);
    }
}
