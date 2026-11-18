package com.pranit.inventory.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Builder
public record ProductPageKey(
        int page,
        int size,
        String keyword,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        String sortDirection
) {
    public ProductPageKey {
        keyword = normalize(keyword);
        category = normalize(category);
        sortBy = (sortBy == null || sortBy.isBlank()) ? "productName" : sortBy;
        sortDirection = "desc".equalsIgnoreCase(sortDirection) ? "desc" : "asc";
        minPrice = minPrice == null ? null : minPrice.stripTrailingZeros();
        maxPrice = maxPrice == null ? null : maxPrice.stripTrailingZeros();
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim().toLowerCase();
        return t.length() > 100 ? t.substring(0, 100) : t;
    }

    private static String enc(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public String asCacheKey() {
        return "p=" + page + "&s=" + size
                + "&k=" + enc(keyword) + "&c=" + enc(category)
                + "&min=" + (minPrice == null ? "" : minPrice.toPlainString())
                + "&max=" + (maxPrice == null ? "" : maxPrice.toPlainString())
                + "&by=" + sortBy + "&dir=" + sortDirection;
    }
}