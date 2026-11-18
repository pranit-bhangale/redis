package com.pranit.inventory.service;

import com.pranit.inventory.dto.ProductRequest;
import com.pranit.inventory.dto.ProductResponse;
import com.pranit.inventory.wrapper.ApiResponse;
import com.pranit.inventory.wrapper.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {
    ApiResponse<PageResponse<ProductResponse>> fetchAllProducts(
            @Min(0) int page, @Min(1) @Max(100) int size,
            String keyword, String category, BigDecimal minPrice,
            BigDecimal maxPrice, String sortBy, String sortDirection
    );

    ApiResponse<ProductResponse> fetchProductById(UUID id);

    ApiResponse<ProductResponse> updateProduct(UUID id, ProductRequest request);

    ApiResponse<Void> deleteProduct(UUID id);
}
