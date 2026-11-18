package com.pranit.inventory.dto;

public record RedisCacheStatsResponse(
        String cacheName,
        long hits,
        long misses,
        long puts,
        long deletes,
        double hitRate
) {
}