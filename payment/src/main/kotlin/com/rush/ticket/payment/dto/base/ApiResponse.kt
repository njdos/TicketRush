package com.rush.ticket.catalog.dto.base

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<out T>(
    val success: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val data: T? = null,
    val error: ApiError? = null
) {
    companion object {
        fun success(data: Nothing? = null): ApiResponse<Nothing?> =
            ApiResponse(success = true, data = data)

        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data)

        fun failure(message: String, details: Any? = null): ApiResponse<Nothing> =
            ApiResponse(success = false, error = ApiError(message, details))
    }

    data class ApiError(
        val message: String,
        val details: Any? = null
    )
}

