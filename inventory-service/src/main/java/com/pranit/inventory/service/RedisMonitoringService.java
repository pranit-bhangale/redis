package com.pranit.inventory.service;

import com.pranit.inventory.dto.RedisCacheEntryResponse;
import com.pranit.inventory.dto.RedisCacheStatsResponse;
import com.pranit.inventory.dto.RedisCacheValueResponse;
import com.pranit.inventory.wrapper.PageResponse;

import java.util.List;

public interface RedisMonitoringService {

    PageResponse<String> getAllCacheKeys(int page, int size);

    PageResponse<RedisCacheEntryResponse> getAllCacheEntries(int page, int size);

    RedisCacheValueResponse getCacheValue(String key);

    Long getCacheTTL(String key);

    List<RedisCacheStatsResponse> getCacheStats();
}