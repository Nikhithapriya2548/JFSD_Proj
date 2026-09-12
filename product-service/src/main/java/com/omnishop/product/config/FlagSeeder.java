package com.omnishop.product.config;

import com.omnishop.product.model.FeatureFlag;
import com.omnishop.product.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Seeds the known flag keys once (idempotent). Toggling happens at runtime
 * via PUT /api/v1/flags/{key} — no redeploy needed for a rollout/rollback.
 */
@Configuration
@RequiredArgsConstructor
public class FlagSeeder implements CommandLineRunner {

    private final FeatureFlagRepository repository;

    @Override
    public void run(String... args) {
        seed("reviews", true, "Product reviews + ratings UI");
        seed("coupons", true, "Coupon field at checkout");
    }

    private void seed(String key, boolean enabled, String description) {
        if (repository.existsById(key)) return;
        repository.save(FeatureFlag.builder()
                .key(key).enabled(enabled).description(description).build());
    }
}
