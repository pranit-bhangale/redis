package com.pranit.inventory.config;

import com.pranit.inventory.properties.CacheProperties;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Adds ±10% random TTL jitter.
     */
    private static RedisCacheWriter.TtlFunction jitteredTtl(Duration base, int percent) {
        return (_, _) -> {
            long millis = base.toMillis();
            long jitter = millis * percent / 100;
            return Duration.ofMillis(millis + ThreadLocalRandom.current().nextLong(-jitter, jitter + 1));
        };
    }

    /**
     * Shared JSON serializer for Redis cache values
     * and monitoring service.
     */
    @Bean
    GenericJacksonJsonRedisSerializer redisValueSerializer() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.pranit.inventory.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
    }

    /**
     * Redis Cache Manager.
     */
    @Bean
    CacheManager cacheManager(
            RedisConnectionFactory factory,
            GenericJacksonJsonRedisSerializer redisValueSerializer,
            CacheProperties props
    ) {
        var defaults = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(props.keyPrefix())
                .entryTtl(jitteredTtl(props.defaultTtl(), props.jitterPercent()))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer));
        Map<String, RedisCacheConfiguration> perCache = props.caches()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e ->
                                defaults.entryTtl(jitteredTtl(e.getValue(), props.jitterPercent()))));
        var writer = RedisCacheWriter.nonLockingRedisCacheWriter(factory, BatchStrategies.scan(props.scanBatchSize()));
        var builder = RedisCacheManager.builder(writer)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .disableCreateOnMissingCache()
                .transactionAware();
        if (props.statistics()) builder.enableStatistics();
        return builder.build();
    }

    /**
     * Fail-open cache error handling.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(@NonNull RuntimeException e, @NonNull Cache c, @NonNull Object k) {
                log.warn("Cache GET failed [{}:{}]: {}", c.getName(), k, e.toString());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException e, @NonNull Cache c, @NonNull Object k, Object v) {
                log.warn("Cache PUT failed [{}:{}]: {}", c.getName(), k, e.toString());
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e, @NonNull Cache c, @NonNull Object k) {
                log.warn("Cache EVICT failed [{}:{}]: {}", c.getName(), k, e.toString());
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException e, @NonNull Cache c) {
                log.warn("Cache CLEAR failed [{}]: {}", c.getName(), e.toString());
            }
        };
    }
}
