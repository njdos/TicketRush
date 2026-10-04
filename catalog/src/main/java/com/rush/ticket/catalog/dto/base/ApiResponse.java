package com.rush.ticket.catalog.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        LocalDateTime timestamp,
        T data,
        ApiError error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, LocalDateTime.now(), data, null);
    }

    public static <T> ApiResponse<T> failure(String message, Object details) {
        return new ApiResponse<>(false, LocalDateTime.now(), null, new ApiError(message, details));
    }

    public record ApiError(String message, Object details) {}
}
