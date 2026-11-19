package com.pranit.inventory.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * All Micrometer usage lives here. Meters are built once and cached,
 * because registry.counter(...) performs a lookup on every call.
 * <p>
 * Tags are LOW-CARDINALITY only: policy name and outcome. Never IP/user/phone.
 */
@Component
public class RateLimitMetrics {

    public static final String OUTCOME_ALLOWED = "allowed";
    public static final String OUTCOME_BLOCKED = "blocked";
    public static final String OUTCOME_ERROR = "error";

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * ratelimit.requests{name, outcome}
     */
    public void request(String policyName, String outcome) {
        counters.computeIfAbsent("req|" + policyName + "|" + outcome, _ ->
                Counter.builder("ratelimit.requests")
                        .description("Rate limiter decisions")
                        .tag("name", policyName)
                        .tag("outcome", outcome)
                        .register(registry)).increment();
    }

    /**
     * ratelimit.key.fallback{name, reason}: a SpEL key was blank/failed so IP was used.
     */
    public void keyFallback(String policyName, String reason) {
        counters.computeIfAbsent("fb|" + policyName + "|" + reason, _ ->
                Counter.builder("ratelimit.key.fallback")
                        .description("Key expression fell back to client IP")
                        .tag("name", policyName)
                        .tag("reason", reason)
                        .register(registry)).increment();
    }

    /**
     * ratelimit.redis.latency{name}: time spent in the Redis script call.
     */
    public Timer redisTimer(String policyName) {
        return timers.computeIfAbsent(policyName, n ->
                Timer.builder("ratelimit.redis.latency")
                        .description("Latency of the rate-limit Redis script call")
                        .tag("name", n)
                        .publishPercentileHistogram()
                        .register(registry)
        );
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }
}