package com.rush.ticket.catalog.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI BASE_URI = URI.create("https://rush-ticket.com/errors");

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        AppErrorCode errorCode = AppErrorCode.CONSTRAINT_VIOLATION;
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, String> invalidParameters = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString().split("\\.")[1],
                        violation -> violation.getMessage(),
                        (existing, replacement) -> existing
                ));

        ProblemDetail problemDetail = createProblemDetail(errorCode, status, errorCode.getDefaultTitle());
        problemDetail.setProperty("invalid_parameters", invalidParameters);

        return buildResponseEntity(problemDetail, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        AppErrorCode errorCode = AppErrorCode.INVALID_INPUT_BODY;
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

        ProblemDetail problemDetail = createProblemDetail(errorCode, status, errorCode.getDefaultTitle());
        problemDetail.setProperty("field_errors", fieldErrors);

        return buildResponseEntity(problemDetail, status);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        AppErrorCode errorCode = AppErrorCode.INVALID_PARAMETER_TYPE;
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String detail = String.format("Parameter '%s' must be of type '%s'", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ProblemDetail problemDetail = createProblemDetail(errorCode, status, detail);

        return buildResponseEntity(problemDetail, status);
    }

    @ExceptionHandler(CatalogDomainException.class)
    public ResponseEntity<Object> handleCatalogDomainException(CatalogDomainException ex, WebRequest request) {
        AppErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        ProblemDetail problemDetail = createProblemDetail(errorCode, status, ex.getMessage());

        return buildResponseEntity(problemDetail, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUnexpectedExceptions(Exception ex, WebRequest request) {
        AppErrorCode errorCode = AppErrorCode.INTERNAL_SERVER_ERROR;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ProblemDetail problemDetail = createProblemDetail(errorCode, status, "An unexpected error occurred. Please contact support.");

        return buildResponseEntity(problemDetail, status);
    }

    private ProblemDetail createProblemDetail(AppErrorCode errorCode, HttpStatus status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(errorCode.getDefaultTitle());
        problemDetail.setType(buildErrorTypeUri(errorCode));
        problemDetail.setProperty("code", errorCode.getCode());
        problemDetail.setProperty("timestamp", Instant.now());

        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString(); // Fallback if traceId is missing
        }
        problemDetail.setProperty("trace_id", traceId);

        return problemDetail;
    }

    private URI buildErrorTypeUri(AppErrorCode errorCode) {
        return URI.create(BASE_URI + "/" + errorCode.name().toLowerCase().replace('_', '-'));
    }

    private ResponseEntity<Object> buildResponseEntity(ProblemDetail problemDetail, HttpStatus status) {
        return ResponseEntity.status(status).body(problemDetail);
    }
}