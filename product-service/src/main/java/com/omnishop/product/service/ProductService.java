package com.omnishop.product.service;

import com.omnishop.product.dto.*;
import java.util.List;

public interface ProductService {
    List<ProductResponseDTO> getAllProducts(String category);
    List<ProductResponseDTO> searchProducts(String q, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, String category, String sortBy);
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO createProduct(ProductRequestDTO request);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO request);
    void deleteProduct(Long id);
    List<ProductResponseDTO> getInStockProducts();
}
