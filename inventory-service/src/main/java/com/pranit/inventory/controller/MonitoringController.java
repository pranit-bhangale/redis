package com.pranit.inventory.controller;

import com.pranit.inventory.dto.RedisCacheEntryResponse;
import com.pranit.inventory.dto.RedisCacheStatsResponse;
import com.pranit.inventory.dto.RedisCacheValueResponse;
import com.pranit.inventory.service.RedisMonitoringService;
import com.pranit.inventory.wrapper.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoring/redis")
@RequiredArgsConstructor
@Validated
public class MonitoringController {

    private final RedisMonitoringService service;

    @GetMapping("/keys")
    public ResponseEntity<PageResponse<String>> getKeys(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(this.service.getAllCacheKeys(page, size));
    }

    @GetMapping("/entries")
    public ResponseEntity<PageResponse<RedisCacheEntryResponse>> getEntries(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(this.service.getAllCacheEntries(page, size));
    }

    @GetMapping("/value")
    public ResponseEntity<RedisCacheValueResponse> getValue(@RequestParam @NotBlank String key) {
        return ResponseEntity.ok(this.service.getCacheValue(key));
    }

    @GetMapping("/ttl")
    public ResponseEntity<Long> getTTL(@RequestParam @NotBlank String key) {
        return ResponseEntity.ok(this.service.getCacheTTL(key));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<RedisCacheStatsResponse>> getStats() {
        return ResponseEntity.ok(this.service.getCacheStats());
    }
}