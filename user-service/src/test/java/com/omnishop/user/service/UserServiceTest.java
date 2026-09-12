package com.omnishop.user.service;

import com.omnishop.user.dto.*;
import com.omnishop.user.model.RefreshToken;
import com.omnishop.user.model.User;
import com.omnishop.user.repository.RefreshTokenRepository;
import com.omnishop.user.repository.UserRepository;
import com.omnishop.user.security.JwtUtil;
import com.omnishop.user.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository repository;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock JwtUtil jwt;
    @Mock LoginRateLimiter rateLimiter;
    @InjectMocks UserService service;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User user(Long id, String email, String rawPassword) {
        return User.builder().id(id).name("Test").email(email)
                .passwordHash(encoder.encode(rawPassword))
                .role(User.Role.CUSTOMER).build();
    }

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setName("New");
        req.setEmail("new@test.com");
        req.setPassword("secret123");
        return req;
    }

    @Test
    void registerHashesPasswordAndDefaultsCustomer() {
        when(repository.existsByEmail("new@test.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponseDTO res = service.register(registerRequest());

        assertThat(res.getId()).isEqualTo(10L);
        assertThat(res.getRole()).isEqualTo("CUSTOMER");
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(repository).save(cap.capture());
        assertThat(encoder.matches("secret123", cap.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerDuplicateRejected() {
        when(repository.existsByEmail("new@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(UserService.EmailAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void loginIssuesTokens() {
        User u = user(3L, "a@test.com", "secret123");
        when(repository.findByEmail("a@test.com")).thenReturn(Optional.of(u));
        when(jwt.generateToken(3L, "CUSTOMER")).thenReturn("ACCESS");
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setEmail("a@test.com");
        req.setPassword("secret123");
        AuthResponse res = service.login(req);

        assertThat(res.getToken()).isEqualTo("ACCESS");
        assertThat(res.getRefreshToken()).isNotBlank();
        assertThat(res.getUser().getRole()).isEqualTo("CUSTOMER");
        verify(rateLimiter).recordSuccess("a@test.com");
    }

    @Test
    void loginWrongPasswordRejected() {
        User u = user(3L, "a@test.com", "secret123");
        when(repository.findByEmail("a@test.com")).thenReturn(Optional.of(u));

        LoginRequest req = new LoginRequest();
        req.setEmail("a@test.com");
        req.setPassword("nope");
        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(UserService.BadCredentialsException.class);
        verify(rateLimiter).recordFailure("a@test.com");
    }

    @Test
    void loginUnknownUserRejected() {
        when(repository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.com");
        req.setPassword("x");
        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(UserService.BadCredentialsException.class);
    }

    @Test
    void refreshRotatesSingleUse() {
        User u = user(3L, "a@test.com", "secret123");
        RefreshToken stored = RefreshToken.builder().userId(3L)
                .tokenHash("H").expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false).build();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(repository.findById(3L)).thenReturn(Optional.of(u));
        when(jwt.generateToken(3L, "CUSTOMER")).thenReturn("ACCESS2");
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = service.refresh("RAW");

        assertThat(res.getToken()).isEqualTo("ACCESS2");
        assertThat(res.getRefreshToken()).isNotBlank();
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refreshRevokedRejected() {
        RefreshToken stored = RefreshToken.builder().userId(3L)
                .tokenHash("H").expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true).build();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("RAW"))
                .isInstanceOf(UserService.BadCredentialsException.class);
    }

    @Test
    void refreshExpiredRejected() {
        RefreshToken stored = RefreshToken.builder().userId(3L)
                .tokenHash("H").expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false).build();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("RAW"))
                .isInstanceOf(UserService.BadCredentialsException.class);
    }

    @Test
    void getByIdMissingIs404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(9L))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }
}
