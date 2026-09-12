package com.omnishop.user.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {
    private String name;
    private String address;
    private String phone;
}
