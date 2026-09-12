package com.omnishop.product.controller;

import com.omnishop.product.model.FeatureFlag;
import com.omnishop.product.repository.FeatureFlagRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Minimal feature flags: kill-switches / rollout toggles without redeploy.
 * Known keys are seeded at startup (FlagSeeder): "reviews", "coupons".
 * Frontend reads this to show/hide the corresponding UI.
 */
@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
@Tag(name = "Feature flags", description = "Runtime toggles (reviews, coupons)")
public class FeatureFlagController {

    private final FeatureFlagRepository repository;

    @GetMapping
    @Operation(summary = "All flags as {key: enabled}")
    public Map<String, Boolean> all() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::isEnabled));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Set a flag (admin)",
            description = "Example: PUT /api/v1/flags/reviews {\"enabled\":false}")
    public FeatureFlag set(@PathVariable String key, @RequestBody Map<String, Boolean> body) {
        FeatureFlag flag = repository.findById(key)
                .orElse(FeatureFlag.builder().key(key).description("").build());
        flag.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
        return repository.save(flag);
    }
}
