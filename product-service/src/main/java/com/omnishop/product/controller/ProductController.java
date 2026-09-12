package com.omnishop.product.controller;

import com.omnishop.product.dto.*;
import com.omnishop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog operations")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products",
            description = "Returns all products, or filters by category when the parameter is given. "
                    + "Example: GET /api/v1/products?category=Electronics")
    @ApiResponse(responseCode = "200", description = "Product list (possibly empty)")
    public List<ProductResponseDTO> getAll(@RequestParam(required = false) String category) {
        return productService.getAllProducts(category);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id", description = "Example: GET /api/v1/products/1")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "No product with that id",
                content = @Content(schema = @Schema(implementation = Object.class)))
    })
    public ProductResponseDTO getById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product",
            description = "Example body: {\"name\":\"Test Phone\",\"description\":\"Demo\","
                    + "\"price\":14999,\"stockQuantity\":20,\"category\":\"Electronics\","
                    + "\"imageUrl\":\"https://...\"}")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ProductResponseDTO create(@Valid @RequestBody ProductRequestDTO request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Replaces all fields of the product with id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "404", description = "No product with that id")
    })
    public ProductResponseDTO update(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete product")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "404", description = "No product with that id")
    })
    public void delete(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @GetMapping("/in-stock")
    @Operation(summary = "List in-stock products",
            description = "Products with stockQuantity > 0, filtered with Java Streams.")
    @ApiResponse(responseCode = "200", description = "In-stock product list")
    public List<ProductResponseDTO> getInStock() {
        return productService.getInStockProducts();
    }

    @GetMapping("/search")
    @Operation(summary = "Search and filter products",
            description = "Example: /api/v1/products/search?q=laptop&minPrice=100&maxPrice=50000"
                    + "&category=Laptops&sortBy=priceAsc. sortBy: priceAsc, priceDesc, "
                    + "ratingDesc, newest. Filters use JPA Specifications (no raw SQL).")
    @ApiResponse(responseCode = "200", description = "Matching product list")
    public List<ProductResponseDTO> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy) {
        return productService.searchProducts(q, minPrice, maxPrice, category, sortBy);
    }
}
