package com.pranit.inventory.model;

import com.pranit.inventory.aop.annotation.RateLimit;
import lombok.Builder;

@Builder
public record RateLimitPolicy(
        String name,
        int limit,
        long windowMs,
        long cooldownMs,
        RateLimit.KeyType key,
        boolean failOpen
) {

    public static RateLimitPolicy from(RateLimit a, String defaultName) {
        long windowMs = a.windowUnit().toMillis(a.window());
        long cooldownMs = a.cooldownUnit().toMillis(a.cooldown());
        // Fail at first use rather than silently misbehaving
        if (a.limit() <= 0 || windowMs <= 0 || cooldownMs < 0) {
            throw new IllegalStateException("Invalid @RateLimit on " + defaultName);
        }
        if (a.key() == RateLimit.KeyType.EXPRESSION && a.keyExpression().isBlank()) {
            throw new IllegalStateException("keyExpression is required when key=EXPRESSION on " + defaultName);
        }
        String name = a.name().isBlank() ? defaultName : a.name();
        return RateLimitPolicy.builder()
                .name(name)
                .limit(a.limit())
                .windowMs(windowMs)
                .cooldownMs(cooldownMs)
                .key(a.key())
                .failOpen(a.failOpen())
                .build();
    }
}



