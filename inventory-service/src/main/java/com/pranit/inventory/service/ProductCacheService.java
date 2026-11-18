package com.pranit.inventory.service;

import com.pranit.inventory.dto.ProductRequest;
import com.pranit.inventory.dto.ProductResponse;
import com.pranit.inventory.entity.Product;
import com.pranit.inventory.exception.ResourceNotFoundException;
import com.pranit.inventory.model.ProductPageKey;
import com.pranit.inventory.repository.ProductRepository;
import com.pranit.inventory.specification.ProductSpecification;
import com.pranit.inventory.wrapper.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProductCacheService {

    private static final List<String> SORTABLE = List.of("productName", "category", "pricePerPiece");
    private final ProductRepository repository;

    @Cacheable(
            cacheNames = "productPages",
            key = "#key.asCacheKey()",
            sync = true
    )
    @Transactional(
            propagation = Propagation.REQUIRED,
            readOnly = true,
            rollbackFor = Exception.class
    )
    public PageResponse<ProductResponse> findPage(final ProductPageKey key) {
        var spec = ProductSpecification.filter(key.keyword(), key.category(), key.minPrice(), key.maxPrice());
        return Page.paginate(this.repository, spec, key.page(), key.size(), key.sortBy(), key.sortDirection(), SORTABLE, ProductResponse::from);
    }

    @Cacheable(
            cacheNames = "products",
            key = "#id",
            sync = true
    )
    @Transactional(
            propagation = Propagation.REQUIRED,
            readOnly = true,
            rollbackFor = Exception.class
    )
    public ProductResponse findById(final UUID id) {
        return this.repository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    @Caching(
            put = {
                    @CachePut(value = "products", key = "#id")
            },
            evict = {
                    @CacheEvict(value = "productPages", allEntries = true)
            }
    )
    public ProductResponse update(final UUID id, final ProductRequest request) {
        final Product product = this.repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (request.productName() != null && !request.productName().isBlank()) {
            product.setProductName(request.productName().trim());
        }
        if (request.category() != null && !request.category().isBlank()) {
            product.setCategory(request.category().trim());
        }
        if (request.pricePerPiece() != null) {
            product.setPricePerPiece(request.pricePerPiece());
        }
        if (request.totalQuantity() != null) {
            final int used = product.getTotalQuantity() - product.getRemaining();
            if (request.totalQuantity() < used) {
                throw new IllegalArgumentException("Total quantity cannot be less than already used quantity");
            }
            product.setTotalQuantity(request.totalQuantity());
            product.setRemaining(request.totalQuantity() - used);
        }
        product.calculateTotalPrice();
        return ProductResponse.from(this.repository.save(product));
    }

    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productPages", allEntries = true)
    })
    public void delete(final UUID id) {
        final Product product = this.repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        this.repository.delete(product);
    }
}