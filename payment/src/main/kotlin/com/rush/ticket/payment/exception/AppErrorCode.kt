package com.rush.ticket.catalog.exception

import org.springframework.http.HttpStatus

enum class AppErrorCode(
    val code: String,
    val httpStatus: HttpStatus,
    val defaultTitle: String
) {
    // Validation & Payload Errors
    INVALID_INPUT_BODY("42001", HttpStatus.BAD_REQUEST, "Input validation failed"),
    CONSTRAINT_VIOLATION("42002", HttpStatus.BAD_REQUEST, "Parameter validation failed"),
    INVALID_PARAMETER_TYPE("42003", HttpStatus.BAD_REQUEST, "Type mismatch on argument processing"),

    // Missing Resources
    EVENT_NOT_FOUND("08404", HttpStatus.NOT_FOUND, "Requested catalog resource does not exist"),

    // Internal Server Faults
    INTERNAL_SERVER_ERROR("XX500", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred on the server.");
}
