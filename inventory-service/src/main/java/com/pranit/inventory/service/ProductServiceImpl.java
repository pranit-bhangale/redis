package com.pranit.inventory.service;

import com.pranit.inventory.dto.ProductRequest;
import com.pranit.inventory.dto.ProductResponse;
import com.pranit.inventory.exception.InvalidResourceException;
import com.pranit.inventory.factory.ApiResponseFactory;
import com.pranit.inventory.model.ProductPageKey;
import com.pranit.inventory.wrapper.ApiResponse;
import com.pranit.inventory.wrapper.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductCacheService cache;
    private final ApiResponseFactory factory;

    @Override
    public ApiResponse<PageResponse<ProductResponse>> fetchAllProducts(
            final int page, final int size,
            final String keyword, final String category,
            final BigDecimal minPrice, final BigDecimal maxPrice,
            final String sortBy, final String sortDirection
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidResourceException("Minimum price cannot be greater than maximum price");
        }
        final var key = ProductPageKey.builder()
                .page(Math.max(page, 0))
                .size(Math.clamp(size, 1, 100))
                .keyword(keyword)
                .category(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        return factory.success("Products fetched successfully", cache.findPage(key));
    }

    @Override
    public ApiResponse<ProductResponse> fetchProductById(final UUID id) {
        return factory.success("Product fetched successfully", cache.findById(id));
    }

    @Override
    public ApiResponse<ProductResponse> updateProduct(final UUID id, final ProductRequest request) {
        return factory.success("Product updated successfully", cache.update(id, request));
    }

    @Override
    public ApiResponse<Void> deleteProduct(final UUID id) {
        cache.delete(id);
        return factory.success("Product deleted successfully", null);
    }
}
