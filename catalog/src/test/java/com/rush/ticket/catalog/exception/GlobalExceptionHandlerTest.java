package com.rush.ticket.catalog.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String ERRORS_BASE = "https://rush-ticket.com/errors/";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = mock(WebRequest.class);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // ---------- ConstraintViolationException ----------

    @Test
    @DisplayName("ConstraintViolation: 400 and invalid_parameters keyed by parameter name")
    void handleConstraintViolation_returnsBadRequestWithParameters() {
        Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();
        violations.add(violation("getEvents.page", "must be positive"));
        violations.add(violation("getEvents.size", "must be less than 100"));

        ResponseEntity<Object> response =
                handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

        ProblemDetail problem = body(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertCommon(problem, AppErrorCode.CONSTRAINT_VIOLATION, HttpStatus.BAD_REQUEST);
        assertThat(problem.getProperties()).containsEntry(
                "invalid_parameters",
                Map.of("page", "must be positive", "size", "must be less than 100"));
    }

    @Test
    @DisplayName("ConstraintViolation: duplicate parameter keeps the first message")
    void handleConstraintViolation_duplicateParameter_keepsFirstMessage() {
        Set<ConstraintViolation<Object>> violations = new LinkedHashSet<>();
        violations.add(violation("getEvents.page", "first"));
        violations.add(violation("getEvents.page", "second"));

        ResponseEntity<Object> response =
                handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

        assertThat(body(response).getProperties())
                .containsEntry("invalid_parameters", Map.of("page", "first"));
    }

    // ---------- MethodArgumentNotValidException ----------

    @Test
    @DisplayName("MethodArgumentNotValid: 400 and field_errors keyed by field name")
    void handleMethodArgumentNotValid_returnsFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("eventRequest", "name", "must not be blank"),
                new FieldError("eventRequest", "date", "must be in the future")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(ex, request);

        ProblemDetail problem = body(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertCommon(problem, AppErrorCode.INVALID_INPUT_BODY, HttpStatus.BAD_REQUEST);
        assertThat(problem.getProperties()).containsEntry(
                "field_errors",
                Map.of("name", "must not be blank", "date", "must be in the future"));
    }

    // ---------- MethodArgumentTypeMismatchException ----------

    @Test
    @DisplayName("TypeMismatch: detail contains parameter name and required type")
    void handleTypeMismatch_describesParameterAndType() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        doReturn(UUID.class).when(ex).getRequiredType();

        ResponseEntity<Object> response = handler.handleTypeMismatch(ex, request);

        ProblemDetail problem = body(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertCommon(problem, AppErrorCode.INVALID_PARAMETER_TYPE, HttpStatus.BAD_REQUEST);
        assertThat(problem.getDetail()).isEqualTo("Parameter 'id' must be of type 'UUID'");
    }

    @Test
    @DisplayName("TypeMismatch: unknown required type falls back to 'unknown'")
    void handleTypeMismatch_nullRequiredType_usesUnknown() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        doReturn(null).when(ex).getRequiredType();

        ResponseEntity<Object> response = handler.handleTypeMismatch(ex, request);

        assertThat(body(response).getDetail()).isEqualTo("Parameter 'id' must be of type 'unknown'");
    }

    // ---------- CatalogDomainException ----------

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    @DisplayName("CatalogDomainException: status, title and code come from its AppErrorCode")
    void handleCatalogDomainException_usesErrorCodeStatus(AppErrorCode errorCode) {
        CatalogDomainException ex = mock(CatalogDomainException.class);
        when(ex.getErrorCode()).thenReturn(errorCode);
        when(ex.getMessage()).thenReturn("Something domain-specific");

        ResponseEntity<Object> response = handler.handleCatalogDomainException(ex, request);

        ProblemDetail problem = body(response);
        HttpStatus expectedStatus = errorCode.getHttpStatus();
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertCommon(problem, errorCode, expectedStatus);
        assertThat(problem.getDetail()).isEqualTo("Something domain-specific");
    }

    // ---------- Any other exception ----------

    @Test
    @DisplayName("Unexpected exception: 500 with generic message, internal details are not leaked")
    void handleAllUnexpectedExceptions_returnsGenericInternalError() {
        ResponseEntity<Object> response =
                handler.handleAllUnexpectedExceptions(new RuntimeException("db password is hunter2"), request);

        ProblemDetail problem = body(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertCommon(problem, AppErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(problem.getDetail())
                .isEqualTo("An unexpected error occurred. Please contact support.")
                .doesNotContain("hunter2");
    }

    // ---------- trace_id ----------

    @Test
    @DisplayName("trace_id is taken from MDC when present")
    void traceId_takenFromMdc() {
        MDC.put("traceId", "trace-123");

        ResponseEntity<Object> response =
                handler.handleAllUnexpectedExceptions(new RuntimeException("x"), request);

        assertThat(body(response).getProperties()).containsEntry("trace_id", "trace-123");
    }

    @Test
    @DisplayName("trace_id falls back to a random UUID when MDC is empty")
    void traceId_generatedWhenMdcEmpty() {
        ResponseEntity<Object> response =
                handler.handleAllUnexpectedExceptions(new RuntimeException("x"), request);

        Object traceId = body(response).getProperties().get("trace_id");
        assertThat(traceId).isInstanceOf(String.class);
        assertThat(UUID.fromString((String) traceId)).isNotNull();
    }

    // ---------- helpers ----------

    private ProblemDetail body(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        return (ProblemDetail) response.getBody();
    }

    private void assertCommon(ProblemDetail problem, AppErrorCode errorCode, HttpStatus status) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getTitle()).isEqualTo(errorCode.getDefaultTitle());
        assertThat(problem.getType()).isEqualTo(
                URI.create(ERRORS_BASE + errorCode.name().toLowerCase().replace('_', '-')));
        assertThat(problem.getProperties())
                .containsEntry("code", errorCode.getCode())
                .containsKey("timestamp")
                .containsKey("trace_id");
        assertThat(problem.getProperties().get("timestamp")).isInstanceOf(Instant.class);
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<Object> violation(String propertyPath, String message) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path(propertyPath));
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private Path path(String value) {
        return new Path() {
            @Override
            public Iterator<Node> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public String toString() {
                return value;
            }
        };
    }
}