package com.pranit.inventory.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Set ratelimit.enabled=false to remove the aspect entirely . Default is on.
 */
@Configuration
@ConditionalOnProperty(name = "ratelimit.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.pranit.inventory.aop")
public class RateLimitConfig {
}