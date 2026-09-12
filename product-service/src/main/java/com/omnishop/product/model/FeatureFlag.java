package com.omnishop.product.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feature_flags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureFlag {
    @Id
    private String key;

    @Column(nullable = false)
    private boolean enabled;

    private String description;
}
