package com.omnishop.user.service;

import com.omnishop.user.dto.*;
import com.omnishop.user.model.RefreshToken;
import com.omnishop.user.model.User;
import com.omnishop.user.repository.RefreshTokenRepository;
import com.omnishop.user.repository.UserRepository;
import com.omnishop.user.security.JwtUtil;
import com.omnishop.user.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repository;
    private final RefreshTokenRepository refreshTokens;
    private final JwtUtil jwt;
    private final LoginRateLimiter rateLimiter;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public UserResponseDTO register(RegisterRequest req) {
        if (repository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .address(req.getAddress())
                .phone(req.getPhone())
                .role(User.Role.CUSTOMER)
                .build();
        User saved = repository.save(user);
        log.info("User registered: id={} email='{}'", saved.getId(), saved.getEmail());
        return toDTO(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        rateLimiter.checkAllowed(req.getEmail());
        User user = repository.findByEmail(req.getEmail())
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(req.getEmail());
                    return new BadCredentialsException();
                });
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email='{}'", req.getEmail());
            rateLimiter.recordFailure(req.getEmail());
            throw new BadCredentialsException();
        }
        rateLimiter.recordSuccess(req.getEmail());
        String token = jwt.generateToken(user.getId(), user.getRole().name());
        String refresh = issueRefreshToken(user.getId());
        log.info("User logged in: id={}", user.getId());
        return AuthResponse.builder()
                .token(token).tokenType("Bearer").refreshToken(refresh).user(toDTO(user)).build();
    }

    /**
     * Rotates access tokens without re-login. The presented refresh token is
     * revoked on use (single-use rotation): the response carries both a new
     * access token and its replacement refresh token.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException());
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BadCredentialsException();
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        User user = repository.findById(stored.getUserId())
                .orElseThrow(() -> new BadCredentialsException());
        String token = jwt.generateToken(user.getId(), user.getRole().name());
        String nextRefresh = issueRefreshToken(user.getId());
        return AuthResponse.builder()
                .token(token).tokenType("Bearer").refreshToken(nextRefresh).user(toDTO(user)).build();
    }

    private String issueRefreshToken(Long userId) {
        String raw = java.util.UUID.randomUUID() + "-" + java.util.UUID.randomUUID();
        refreshTokens.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256(raw))
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build());
        return raw;
    }

    private static String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long id) {
        return toDTO(repository.findById(id).orElseThrow(() -> new UserNotFoundException(id)));
    }

    @Transactional
    public UserResponseDTO update(Long id, UpdateUserRequest req) {
        User user = repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        if (req.getName() != null) user.setName(req.getName());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        return toDTO(repository.save(user));
    }

    private UserResponseDTO toDTO(User u) {
        return UserResponseDTO.builder()
                .id(u.getId()).name(u.getName()).email(u.getEmail())
                .address(u.getAddress()).phone(u.getPhone())
                .role(u.getRole().name()).createdAt(u.getCreatedAt()).build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(Long id) { super("User not found: " + id); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) { super("Email already registered: " + email); }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException() { super("Invalid email or password"); }
    }
}
