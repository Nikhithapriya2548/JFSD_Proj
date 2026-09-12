package com.omnishop.product.controller;

import com.omnishop.product.dto.*;
import com.omnishop.product.exception.ProductNotFoundException;
import com.omnishop.product.model.Review;
import com.omnishop.product.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.cache.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product ratings and comments")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @PostMapping("/api/v1/products/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productList", allEntries = true)})
    @Operation(summary = "Add review",
            description = "Example: POST /api/v1/products/1/reviews "
                    + "{\"userId\":1,\"rating\":5,\"comment\":\"Great laptop!\"}")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Review added"),
        @ApiResponse(responseCode = "400", description = "Rating must be 1-5"),
        @ApiResponse(responseCode = "404", description = "No product with that id")
    })
    public ReviewResponse add(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        if (!productRepository.existsById(id)) throw new ProductNotFoundException(id);
        Review saved = reviewRepository.save(Review.builder()
                .productId(id).userId(req.getUserId())
                .rating(req.getRating()).comment(req.getComment()).build());
        log.info("Review added: product={} user={} rating={}", id, req.getUserId(), req.getRating());
        return toDTO(saved);
    }

    @GetMapping("/api/v1/products/{id}/reviews")
    @Operation(summary = "List reviews for a product")
    @ApiResponse(responseCode = "200", description = "Review list")
    public List<ReviewResponse> list(@PathVariable Long id) {
        return reviewRepository.findByProductId(id).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ReviewResponse toDTO(Review r) {
        return ReviewResponse.builder()
                .id(r.getId()).productId(r.getProductId()).userId(r.getUserId())
                .rating(r.getRating()).comment(r.getComment()).createdAt(r.getCreatedAt()).build();
    }
}
