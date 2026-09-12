package com.omnishop.order.service;

import com.omnishop.order.model.Coupon;
import com.omnishop.order.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.math.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);
    private final CouponRepository repository;

    public record Validation(BigDecimal discount, String reason) {}

    /** Validates without consuming. Throws with a human message when unusable. */
    @Transactional(readOnly = true)
    public Validation validate(String code, BigDecimal subtotal) {
        Coupon c = repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new InvalidCouponException("Unknown coupon code: " + code));
        if (c.getExpiryDate() != null && c.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCouponException("Coupon " + code + " expired on " + c.getExpiryDate().toLocalDate());
        }
        if (c.getUsageLimit() != null && c.getTimesUsed() >= c.getUsageLimit()) {
            throw new InvalidCouponException("Coupon " + code + " has reached its usage limit");
        }
        if (c.getMinOrderAmount() != null && subtotal.compareTo(c.getMinOrderAmount()) < 0) {
            throw new InvalidCouponException("Coupon " + code + " needs a minimum order of $" + c.getMinOrderAmount());
        }
        BigDecimal discount = c.getDiscountType() == Coupon.DiscountType.PERCENTAGE
                ? subtotal.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getValue().min(subtotal);
        return new Validation(discount, "OK");
    }

    @Transactional
    public Validation consume(String code, BigDecimal subtotal) {
        Validation v = validate(code, subtotal);
        Coupon c = repository.findByCodeIgnoreCase(code).orElseThrow();
        c.setTimesUsed(c.getTimesUsed() + 1);
        repository.save(c);
        log.info("Coupon consumed: {} discount={}", code, v.discount());
        return v;
    }

    @Transactional
    public Coupon create(Coupon coupon) {
        coupon.setTimesUsed(0);
        return repository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<Coupon> all() {
        return repository.findAll();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidCouponException extends RuntimeException {
        public InvalidCouponException(String msg) { super(msg); }
    }
}
