package com.omnishop.notification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String eventType;  // order.created, payment.completed, ...

    @Column(nullable = false)
    private Long refId;  // orderId / productId the event is about

    // The would-be email/SMS body — logged, never actually sent
    // (no real provider; same reasoning as the mock payment gateway)
    @Column(length = 2048)
    private String message;

    private LocalDateTime createdAt = LocalDateTime.now();
}
