package com.omnishop.product.service;

import com.omnishop.product.dto.*;
import com.omnishop.product.exception.ProductNotFoundException;
import com.omnishop.product.model.Product;
import com.omnishop.product.model.Review;
import com.omnishop.product.repository.ProductRepository;
import com.omnishop.product.repository.ReviewRepository;
import com.omnishop.product.spec.ProductSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository repository;
    private final ReviewRepository reviewRepository;

    @Override
    @Cacheable(value = "productList", keyGenerator = "categoryKeyGenerator")
    public List<ProductResponseDTO> getAllProducts(String category) {
        List<Product> products = (category == null) ? repository.findAll() : repository.findByCategory(category);
        return products.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDTO getProductById(Long id) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return mapToResponseDTO(product);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productList", allEntries = true)})
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();
        Product saved = repository.save(product);
        log.info("Product created: id={} name='{}' price={}", saved.getId(), saved.getName(), saved.getPrice());
        return mapToResponseDTO(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productList", allEntries = true)})
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        return mapToResponseDTO(repository.save(product));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productList", allEntries = true)})
    public void deleteProduct(Long id) {
        if (!repository.existsById(id)) throw new ProductNotFoundException(id);
        repository.deleteById(id);
    }

    @Override
    public List<ProductResponseDTO> getInStockProducts() {
        // Requirement: Use Java Streams explicitly to filter in-stock products
        return repository.findAll().stream()
                .filter(p -> p.getStockQuantity() > 0)
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponseDTO> searchProducts(String q, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, String category, String sortBy) {
        Specification<Product> spec = Specification
                .where(ProductSpecs.textSearch(q))
                .and(ProductSpecs.priceBetween(minPrice, maxPrice))
                .and(ProductSpecs.categoryIs(category));

        // price/newest via JPA Sort; ratingDesc needs the computed average → Streams sort
        boolean byRating = "ratingDesc".equalsIgnoreCase(sortBy);
        Sort sort = switch (sortBy == null ? "" : sortBy) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.unsorted();
        };
        List<ProductResponseDTO> results = repository.findAll(spec, sort)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        if (byRating) {
            results = results.stream()
                    .sorted(Comparator.comparing(
                            (ProductResponseDTO d) -> d.getAverageRating() == null ? 0.0 : d.getAverageRating())
                            .reversed())
                    .collect(Collectors.toList());
        } else if (!sort.isSorted() && q != null && !q.isBlank()) {
            // Relevance fallback: name match beats category beats description.
            // (DB-agnostic Streams scoring — no native full-text index needed
            // at this catalog size; revisit past ~10k products.)
            String needle = q.trim().toLowerCase();
            results = results.stream()
                    .sorted(Comparator.comparingInt((ProductResponseDTO d) ->
                            -relevance(d, needle))
                            .thenComparing(ProductResponseDTO::getName,
                                    Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        }
        return results;
    }

    private int relevance(ProductResponseDTO d, String needle) {
        int score = 0;
        if (d.getName() != null && d.getName().toLowerCase().contains(needle)) score += 3;
        if (d.getCategory() != null && d.getCategory().toLowerCase().contains(needle)) score += 2;
        if (d.getDescription() != null && d.getDescription().toLowerCase().contains(needle)) score += 1;
        return score;
    }

    private ProductResponseDTO mapToResponseDTO(Product p) {
        // Real aggregation with Streams: average of all ratings for this product
        List<Review> reviews = reviewRepository.findByProductId(p.getId());
        Double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(Double.NaN);
        return ProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .category(p.getCategory())
                .imageUrl(p.getImageUrl())
                .averageRating(Double.isNaN(avg) ? null : Math.round(avg * 10.0) / 10.0)
                .reviewCount(reviews.size())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
