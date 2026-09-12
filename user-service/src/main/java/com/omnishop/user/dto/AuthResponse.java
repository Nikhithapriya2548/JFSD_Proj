package com.omnishop.user.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private String refreshToken;
    private UserResponseDTO user;
}
