package com.pranit.inventory.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)                 // method-level only
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RateLimits.class)               // allows stacking several limits on one method
public @interface RateLimit {

    /**
     * Max requests per window.
     */
    int limit() default 5;

    long window() default 60;

    TimeUnit windowUnit() default TimeUnit.SECONDS;

    /**
     * Lockout after the limit is exceeded. 0 = no cooldown (pure sliding window).
     */
    long cooldown() default 30;

    TimeUnit cooldownUnit() default TimeUnit.SECONDS;

    KeyType key() default KeyType.IP;

    /**
     * SpEL, used when key = EXPRESSION. Method parameters are available by name,
     * e.g. "#req.phone". A null/blank result falls back to the client IP.
     */
    String keyExpression() default "";

    /**
     * Bucket name (also the metric tag). Empty = "Class#method#index". Must be unique per bucket.
     */
    String name() default "";

    /**
     * true = allow when Redis is down; false = reject with 503 (use for login/OTP).
     */
    boolean failOpen() default true;

    enum KeyType {IP, USER, USER_OR_IP, EXPRESSION}
}