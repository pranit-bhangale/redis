package com.pranit.inventory.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RedisCacheKeysResponse(
        int totalKeys,
        List<String> keys
) {
}
