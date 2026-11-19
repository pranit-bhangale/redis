package com.pranit.inventory.service;

import com.pranit.inventory.model.RateLimitPolicy;
import com.pranit.inventory.model.RateLimitResult;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final StringRedisTemplate redis;
    private final RateLimitMetrics metrics;
    private final DefaultRedisScript<List<Long>> script = loadScript();

    public RateLimitService(StringRedisTemplate redis, RateLimitMetrics metrics) {
        this.redis = redis;
        this.metrics = metrics;
    }

    @SuppressWarnings("unchecked")
    private static DefaultRedisScript<List<Long>> loadScript() {
        var s = new DefaultRedisScript<List<Long>>();
        s.setLocation(new ClassPathResource("lua/rate_limiting.lua"));
        s.setResultType((Class<List<Long>>) (Class<?>) List.class);
        return s;
    }

    public RateLimitResult check(RateLimitPolicy policy, String scope, String id) {
        String base = "rl:{" + policy.name() + ":" + scope + ":" + id + "}";
        List<String> keys = List.of(base + ":w", base + ":c");
        Timer.Sample sample = metrics.startTimer();
        try {
            List<Long> reply = redis.execute(script, keys,
                    Long.toString(policy.windowMs()),
                    Integer.toString(policy.limit()),
                    Long.toString(policy.cooldownMs()),
                    UUID.randomUUID().toString());
            if (reply.size() < 3) {
                throw new IllegalStateException("Unexpected script reply: " + reply);
            }

            boolean allowed = reply.get(0) == 1L;
            metrics.request(policy.name(), allowed
                    ? RateLimitMetrics.OUTCOME_ALLOWED
                    : RateLimitMetrics.OUTCOME_BLOCKED);
            return RateLimitResult.builder()
                    .allowed(allowed)
                    .retryAfterMs(reply.get(1))
                    .remaining(reply.get(2))
                    .build();

        } catch (DataAccessException | IllegalStateException ex) {
            metrics.request(policy.name(), RateLimitMetrics.OUTCOME_ERROR);
            if (policy.failOpen()) {
                log.warn("Rate limiter unavailable for '{}', failing open: {}",
                        policy.name(), ex.toString());
                return RateLimitResult.permit(-1);
            }
            log.error("Rate limiter unavailable for '{}', failing closed", policy.name(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service temporarily unavailable");
        } finally {
            sample.stop(metrics.redisTimer(policy.name()));
        }
    }
}