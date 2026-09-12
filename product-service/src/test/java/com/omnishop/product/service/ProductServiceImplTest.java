package com.omnishop.product.service;

import com.omnishop.product.exception.ProductNotFoundException;
import com.omnishop.product.model.*;
import com.omnishop.product.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock ProductRepository repository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks ProductServiceImpl service;

    private Product product(Long id) {
        return Product.builder().id(id).name("Widget")
                .price(new BigDecimal("99.99")).stockQuantity(5).build();
    }

    private Review review(Long productId, int rating) {
        return Review.builder().productId(productId).userId(1L).rating(rating).build();
    }

    @Test
    void averageRatingRoundedToOneDecimal() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1L)));
        when(reviewRepository.findByProductId(1L))
                .thenReturn(List.of(review(1L, 5), review(1L, 4), review(1L, 4)));

        var dto = service.getProductById(1L);

        assertThat(dto.getAverageRating()).isEqualTo(4.3);
        assertThat(dto.getReviewCount()).isEqualTo(3);
    }

    @Test
    void noReviewsMeansNullAverage() {
        when(repository.findById(2L)).thenReturn(Optional.of(product(2L)));
        when(reviewRepository.findByProductId(2L)).thenReturn(List.of());

        var dto = service.getProductById(2L);

        assertThat(dto.getAverageRating()).isNull();
        assertThat(dto.getReviewCount()).isZero();
    }

    @Test
    void missingProductThrows() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProductById(9L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void relevanceOrdersNameAboveCategoryAboveDescription() {
        Product nameHit = Product.builder().id(1L).name("Phone X")
                .description("gadget").category("Gadgets")
                .price(BigDecimal.ONE).stockQuantity(1).build();
        Product categoryHit = Product.builder().id(2L).name("Tablet Y")
                .description("gadget").category("Phones & Accessories")
                .price(BigDecimal.ONE).stockQuantity(1).build();
        Product descHit = Product.builder().id(3L).name("Tablet Z")
                .description("a great phone companion").category("Gadgets")
                .price(BigDecimal.ONE).stockQuantity(1).build();
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(descHit, categoryHit, nameHit));
        lenient().when(reviewRepository.findByProductId(anyLong())).thenReturn(List.of());

        var results = service.searchProducts("phone", null, null, null, null);

        assertThat(results.stream().map(v -> v.getId()).toList())
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void explicitSortSkipsRelevance() {
        Product b = Product.builder().id(2L).name("B-Phone")
                .price(new BigDecimal("20")).stockQuantity(1).build();
        Product a = Product.builder().id(1L).name("A-Phone")
                .price(new BigDecimal("10")).stockQuantity(1).build();
        // Repository already applied priceAsc; relevance must not reshuffle
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(a, b));
        lenient().when(reviewRepository.findByProductId(anyLong())).thenReturn(List.of());

        var results = service.searchProducts("phone", null, null, null, "priceAsc");

        assertThat(results.stream().map(v -> v.getId()).toList())
                .containsExactly(1L, 2L);
    }
}
