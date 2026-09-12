package com.omnishop.order.controller;

import com.omnishop.order.model.Coupon;
import com.omnishop.order.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Discount codes applied at order creation")
public class CouponController {

    private final CouponService coupons;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create coupon (admin)",
            description = "Example: {\"code\":\"SAVE10\",\"discountType\":\"PERCENTAGE\","
                    + "\"value\":10,\"minOrderAmount\":50,\"usageLimit\":100}")
    public Coupon create(@Valid @RequestBody Coupon coupon) {
        return coupons.create(coupon);
    }

    @GetMapping
    @Operation(summary = "List coupons")
    public List<Coupon> all() {
        return coupons.all();
    }

    @GetMapping("/validate")
    @Operation(summary = "Live-validate a coupon for the checkout page",
            description = "Example: /api/v1/coupons/validate?code=SAVE10&subtotal=120. "
                    + "Returns {valid, discount, reason} — never throws for bad codes.")
    public Map<String, Object> validate(@RequestParam String code, @RequestParam BigDecimal subtotal) {
        try {
            var v = coupons.validate(code, subtotal);
            return Map.of("valid", true, "discount", v.discount(), "reason", "OK");
        } catch (CouponService.InvalidCouponException e) {
            return Map.of("valid", false, "discount", BigDecimal.ZERO, "reason", e.getMessage());
        }
    }
}
