package com.omnishop.user.controller;

import com.omnishop.user.dto.*;
import com.omnishop.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Registration, login (JWT), profile")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register",
            description = "Example: {\"name\":\"Asha\",\"email\":\"asha@mail.com\","
                    + "\"password\":\"secret123\",\"address\":\"Hyderabad\",\"phone\":\"999\"}. "
                    + "Password is BCrypt-hashed; role defaults to CUSTOMER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public UserResponseDTO register(@Valid @RequestBody RegisterRequest req) {
        return userService.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Returns a JWT (userId + role claims). "
                    + "Example: {\"email\":\"asha@mail.com\",\"password\":\"secret123\"}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated, token issued"),
        @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate access token",
            description = "Single-use rotation: the refresh token is revoked and replaced. "
                    + "Example: {\"refreshToken\":\"...\"}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New token pair issued"),
        @ApiResponse(responseCode = "401", description = "Unknown, revoked or expired refresh token")
    })
    public AuthResponse refresh(@RequestBody java.util.Map<String, String> body) {
        return userService.refresh(body.get("refreshToken"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "No user with that id")
    })
    public UserResponseDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update profile (name, address, phone)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    public UserResponseDTO update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }
}
