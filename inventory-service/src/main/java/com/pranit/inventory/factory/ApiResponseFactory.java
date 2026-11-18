package com.pranit.inventory.factory;

import com.pranit.inventory.wrapper.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ApiResponseFactory {

    public static final String START_TIMESTAMP = "apiStartTimestamp";
    private final HttpServletRequest request;

    public <T> ApiResponse<T> success(final String message, final T data) {
        final Instant startTimestamp = (Instant) request.getAttribute(START_TIMESTAMP);
        final Instant endTimestamp = Instant.now();
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .data(data)
                .startTimestamp(startTimestamp)
                .endTimestamp(endTimestamp)
                .latencyMs(startTimestamp != null
                        ? Duration.between(startTimestamp, endTimestamp).toMillis()
                        : 0L)
                .build();
    }
}
