package com.omnishop.product.controller;

import com.omnishop.product.model.FeatureFlag;
import com.omnishop.product.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagControllerTest {

    @Mock FeatureFlagRepository repository;
    @InjectMocks FeatureFlagController controller;

    @Test
    void allReturnsKeyMap() {
        when(repository.findAll()).thenReturn(List.of(
                FeatureFlag.builder().key("reviews").enabled(true).build(),
                FeatureFlag.builder().key("coupons").enabled(false).build()));

        Map<String, Boolean> all = controller.all();

        assertThat(all).containsEntry("reviews", true).containsEntry("coupons", false);
    }

    @Test
    void setUpdatesExisting() {
        FeatureFlag existing = FeatureFlag.builder().key("reviews").enabled(true).build();
        when(repository.findById("reviews")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        FeatureFlag out = controller.set("reviews", Map.of("enabled", false));

        assertThat(out.isEnabled()).isFalse();
    }

    @Test
    void setCreatesUnknownKey() {
        when(repository.findById("fresh")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        FeatureFlag out = controller.set("fresh", Map.of("enabled", true));

        assertThat(out.getKey()).isEqualTo("fresh");
        assertThat(out.isEnabled()).isTrue();
    }
}
