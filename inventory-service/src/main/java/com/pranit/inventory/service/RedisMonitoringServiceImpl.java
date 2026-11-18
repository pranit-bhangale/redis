package com.pranit.inventory.service;

import com.pranit.inventory.dto.RedisCacheEntryResponse;
import com.pranit.inventory.dto.RedisCacheStatsResponse;
import com.pranit.inventory.dto.RedisCacheValueResponse;
import com.pranit.inventory.wrapper.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class RedisMonitoringServiceImpl implements RedisMonitoringService {

    private static final String KEY_PREFIX = "v1:";
    private static final int MAX_PAGE_SIZE = 100;

    private final RedisConnectionFactory connectionFactory;
    private final GenericJacksonJsonRedisSerializer redisValueSerializer;
    private final CacheManager cacheManager;

    /**
     * Retrieve application cache keys with pagination.
     */
    @Override
    public PageResponse<String> getAllCacheKeys(int page, int size) {
        validatePagination(page, size);
        List<String> keys = scanKeys();
        int fromIndex = Math.min(page * size, keys.size());
        int toIndex = Math.min(fromIndex + size, keys.size());
        List<String> contents = keys.subList(fromIndex, toIndex);
        int totalPages = calculateTotalPages(keys.size(), size);
        return PageResponse.<String>builder()
                .contents(contents)
                .currentPage(page)
                .pageSize(size)
                .totalElements(keys.size())
                .totalPages(totalPages)
                .isFirstPage(page == 0)
                .isLastPage(page >= totalPages - 1)
                .build();
    }

    /**
     * Retrieve cache entries with values and TTL using pagination.
     */
    @Override
    public PageResponse<RedisCacheEntryResponse> getAllCacheEntries(int page, int size) {
        validatePagination(page, size);
        List<String> keys = scanKeys();
        int fromIndex = Math.min(page * size, keys.size());
        int toIndex = Math.min(fromIndex + size, keys.size());
        List<RedisCacheEntryResponse> entries = new ArrayList<>();
        try (RedisConnection connection = this.connectionFactory.getConnection()) {
            for (String key : keys.subList(fromIndex, toIndex)) {
                byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
                byte[] rawValue = connection.stringCommands().get(rawKey);
                if (rawValue == null) continue;
                Object value = this.redisValueSerializer.deserialize(rawValue);
                Long ttl = connection.keyCommands().ttl(rawKey);
                entries.add(new RedisCacheEntryResponse(key, extractCacheName(key), value, ttl));
            }
        }
        int totalPages = calculateTotalPages(keys.size(), size);
        return PageResponse.<RedisCacheEntryResponse>builder()
                .contents(entries)
                .currentPage(page)
                .pageSize(size)
                .totalElements(keys.size())
                .totalPages(totalPages)
                .isFirstPage(page == 0)
                .isLastPage(page >= totalPages - 1)
                .build();
    }

    /**
     * Retrieve a particular cache value.
     */
    @Override
    public RedisCacheValueResponse getCacheValue(String key) {
        validateKey(key);
        try (RedisConnection connection = this.connectionFactory.getConnection()) {
            byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
            byte[] rawValue = connection.stringCommands().get(rawKey);
            if (rawValue == null) {
                throw new ResponseStatusException(NOT_FOUND, "Redis cache key not found: " + key);
            }
            Object value = this.redisValueSerializer.deserialize(rawValue);
            Long ttl = connection.keyCommands().ttl(rawKey);
            return new RedisCacheValueResponse(key, value, ttl);
        }
    }

    /**
     * Retrieve remaining TTL of a particular key.
     */
    @Override
    public Long getCacheTTL(String key) {
        validateKey(key);
        try (RedisConnection connection = this.connectionFactory.getConnection()) {
            byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
            Long ttl = connection.keyCommands().ttl(rawKey);
            if (ttl == null || ttl == -2) {
                throw new ResponseStatusException(NOT_FOUND, "Redis cache key not found: " + key);
            }
            return ttl;
        }
    }

    /**
     * Retrieve cache hit/miss statistics.
     */
    @Override
    public List<RedisCacheStatsResponse> getCacheStats() {
        List<RedisCacheStatsResponse> result = new ArrayList<>();
        for (String cacheName : this.cacheManager.getCacheNames()) {
            Cache cache = this.cacheManager.getCache(cacheName);
            if (cache instanceof TransactionAwareCacheDecorator decorator) {
                cache = decorator.getTargetCache();
            }
            if (!(cache instanceof RedisCache redisCache)) continue;
            var stats = redisCache.getStatistics();
            long hits = stats.getHits();
            long misses = stats.getMisses();
            long total = hits + misses;
            double hitRate = total == 0 ? 0.0 : (hits * 100.0) / total;
            result.add(new RedisCacheStatsResponse(cacheName, hits, misses, stats.getPuts(), stats.getDeletes(), hitRate));
        }
        return result;
    }

    /**
     * Scan Redis keys using SCAN.
     */
    private List<String> scanKeys() {
        Set<String> keys = new TreeSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(KEY_PREFIX + "*")
                .count(1000)
                .build();
        try (RedisConnection connection = this.connectionFactory.getConnection();
             Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                if (isValidCacheKey(key)) keys.add(key);
            }
        }
        return new ArrayList<>(keys);
    }

    /**
     * Extract cache name from Redis key.
     * <p>
     * Example: v1:users::101 -> users
     */
    private String extractCacheName(String key) {
        String cacheKey = key.substring(KEY_PREFIX.length());
        int separatorIndex = cacheKey.indexOf("::");
        return separatorIndex >= 0 ? cacheKey.substring(0, separatorIndex) : cacheKey;
    }

    /**
     * Validate Redis cache key.
     */
    private void validateKey(String key) {
        if (!isValidCacheKey(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Redis cache key");
        }
    }

    /**
     * Check whether key belongs to a registered application cache.
     */
    private boolean isValidCacheKey(String key) {
        if (key == null || !key.startsWith(KEY_PREFIX)) return false;
        String cacheKey = key.substring(KEY_PREFIX.length());
        int separatorIndex = cacheKey.indexOf("::");
        if (separatorIndex <= 0) return false;
        String cacheName = cacheKey.substring(0, separatorIndex);
        return this.cacheManager.getCacheNames().contains(cacheName);
    }

    /**
     * Validate pagination parameters.
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("Page number is too large");
        }
    }

    /**
     * Calculate total pages.
     */
    private int calculateTotalPages(long totalKeys, int size) {
        return (int) ((totalKeys + size - 1) / size);
    }
}
