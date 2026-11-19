package com.pranit.inventory.model;

import lombok.Builder;

@Builder
public record RateLimitResult(
        boolean allowed,
        long retryAfterMs,
        long remaining
) {
    /**
     * remaining = -1 means "unknown" (limiter bypassed because Redis is down).
     */
    public static RateLimitResult permit(long remaining) {
        return RateLimitResult.builder()
                .allowed(true)
                .retryAfterMs(0)
                .remaining(remaining)
                .build();
    }
}