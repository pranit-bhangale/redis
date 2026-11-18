package com.pranit.inventory.dto;

public record RedisCacheEntryResponse(
        String key,
        String cacheName,
        Object value,
        Long ttlSeconds
) {
}