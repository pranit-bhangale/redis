package com.pranit.inventory.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
public class ApiExecutionInterceptor implements HandlerInterceptor {

    public static final String START_TIMESTAMP = "apiStartTimestamp";

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final @NonNull HttpServletResponse response,
            final @NonNull Object handler
    ) {
        request.setAttribute(START_TIMESTAMP, Instant.now());
        return true;
    }
}