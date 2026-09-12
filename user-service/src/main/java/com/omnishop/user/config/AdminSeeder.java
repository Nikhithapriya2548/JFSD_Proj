package com.omnishop.user.config;

import com.omnishop.user.model.User;
import com.omnishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Seeds exactly one ADMIN account from environment on startup (idempotent).
 * Registration stays CUSTOMER-only — this is the sole path to an admin.
 * Override the insecure dev defaults via ADMIN_EMAIL / ADMIN_PASSWORD.
 */
@Configuration
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${ADMIN_EMAIL:admin@omnishop.local}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (repository.existsByEmail(adminEmail)) {
            return;
        }
        repository.save(User.builder()
                .name("Administrator")
                .email(adminEmail)
                .passwordHash(encoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .build());
        if ("admin@omnishop.local".equals(adminEmail) || "admin123".equals(adminPassword)) {
            log.warn("Seeded default ADMIN account ({}). Set ADMIN_EMAIL/ADMIN_PASSWORD env vars in production!",
                    adminEmail);
        } else {
            log.info("Seeded ADMIN account for {}", adminEmail);
        }
    }
}
