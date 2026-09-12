package com.omnishop.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewRequest {
    @NotNull private Long userId;
    @NotNull @Min(1) @Max(5) private Integer rating;
    @Size(max = 2000) private String comment;
}
