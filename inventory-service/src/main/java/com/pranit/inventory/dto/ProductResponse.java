package com.pranit.inventory.dto;

import com.pranit.inventory.entity.Product;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductResponse(
        UUID id,
        String productName,
        String category,
        BigDecimal pricePerPiece,
        Integer totalQuantity,
        Integer remaining,
        BigDecimal totalPrice,
        Long version
) {

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .category(product.getCategory())
                .pricePerPiece(product.getPricePerPiece())
                .totalQuantity(product.getTotalQuantity())
                .remaining(product.getRemaining())
                .totalPrice(product.getTotalPrice())
                .version(product.getVersion())
                .build();
    }
}