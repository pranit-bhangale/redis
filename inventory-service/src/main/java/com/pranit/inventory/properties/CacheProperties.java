package com.pranit.inventory.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "inventory.cache")
public record CacheProperties(
        @DefaultValue("v1:") String keyPrefix,
        @DefaultValue("10m") Duration defaultTtl,
        @DefaultValue("10") int jitterPercent,
        @DefaultValue("1000") int scanBatchSize,
        @DefaultValue("false") boolean statistics,
        Map<String, Duration> caches) {

    public CacheProperties {
        caches = caches == null ? Map.of() : caches;
    }
}
