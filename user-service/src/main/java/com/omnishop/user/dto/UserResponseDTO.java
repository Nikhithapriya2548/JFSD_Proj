package com.omnishop.user.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String address;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}
