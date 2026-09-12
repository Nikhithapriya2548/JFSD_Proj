package com.omnishop.notification.controller;

import com.omnishop.notification.model.Notification;
import com.omnishop.notification.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Simulated notification inbox")
public class NotificationController {

    private final NotificationRepository repository;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Notification inbox for a user (newest first)")
    public List<Notification> byUser(@PathVariable Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
