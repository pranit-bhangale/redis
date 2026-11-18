package com.pranit.inventory.dto;

public record RedisCacheValueResponse(
        String key,
        Object value,
        Long ttlSeconds
) {
}
