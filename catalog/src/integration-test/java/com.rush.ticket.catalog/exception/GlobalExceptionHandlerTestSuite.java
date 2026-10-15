package com.rush.ticket.catalog.exception;

import com.rush.ticket.catalog.dto.base.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTestSuite {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAppErrorCodeAndExceptionGetters() {
        UUID id = UUID.randomUUID();
        EventNotFoundException ex = new EventNotFoundException(id);

        Assertions.assertEquals(AppErrorCode.EVENT_NOT_FOUND, ex.getErrorCode());
        assertEquals(String.format("Event with ID '%s' was not found", id), ex.getMessage());

        String customMessage = "Custom error message for catalog";
        EventNotFoundException exWithCustomMessage = new EventNotFoundException(customMessage);

        assertEquals(AppErrorCode.EVENT_NOT_FOUND, exWithCustomMessage.getErrorCode());
        assertEquals(customMessage, exWithCustomMessage.getMessage());

        assertEquals("08404", AppErrorCode.EVENT_NOT_FOUND.getCode());
        assertNotNull(AppErrorCode.EVENT_NOT_FOUND.getHttpStatus());
        assertNotNull(AppErrorCode.EVENT_NOT_FOUND.getDefaultTitle());
    }

    @Test
    void handleCatalogDomainException_ReturnsCorrectProblemDetail() throws Exception {
        mockMvc.perform(get("/test-exception/domain")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Requested catalog resource does not exist")))
                .andExpect(jsonPath("$.code", is("08404")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace_id", notNullValue()));
    }

    @Test
    void handleTypeMismatch_ReturnsFormattedDetailMessage() throws Exception {
        mockMvc.perform(get("/test-exception/type-mismatch")
                        .param("id", "invalid-uuid-string")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("42003")))
                .andExpect(jsonPath("$.detail", containsString("Parameter 'id' must be of type 'UUID'")));
    }

    @Test
    void handleAllUnexpectedExceptions_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test-exception/unexpected")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is("XX500")))
                .andExpect(jsonPath("$.detail", containsString("An unexpected error occurred. Please contact support.")));
    }

    @Test
    void handleConstraintViolation_ReturnsInvalidParametersMap() throws Exception {
        mockMvc.perform(get("/test-exception/constraint")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("42002")))
                .andExpect(jsonPath("$.title", is("Parameter validation failed")))
                .andExpect(jsonPath("$.invalid_parameters.price", is("Price cannot be negative")));
    }

    @Test
    void createProblemDetail_WithExistingTraceId_UsesLogContextTraceId() throws Exception {
        try {
            String activeTraceId = "custom-active-trace-12345";
            MDC.put("traceId", activeTraceId);

            mockMvc.perform(get("/test-exception/unexpected")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.trace_id", is(activeTraceId)));
        } finally {
            MDC.clear();
        }
    }

    @Test
    void success_ShouldCreateValidSuccessResponse() {
        String sampleData = "Event Details Data";
        ApiResponse<String> response = ApiResponse.success(sampleData);

        assertTrue(response.success());
        assertNotNull(response.timestamp());
        assertEquals(sampleData, response.data());
        assertNull(response.error());
    }

    @Test
    void failure_ShouldCreateValidFailureResponseWithApiError() {
        String errorMessage = "Validation Failed";
        String errorDetails = "Field 'name' cannot be blank";
        ApiResponse<Void> response = ApiResponse.failure(errorMessage, errorDetails);

        assertFalse(response.success());
        assertNotNull(response.timestamp());
        assertNull(response.data());
        assertNotNull(response.error());
        assertEquals(errorMessage, response.error().message());
        assertEquals(errorDetails, response.error().details());
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/test-exception/domain")
        public void throwDomainException() {
            throw new EventNotFoundException(UUID.randomUUID());
        }

        @GetMapping("/test-exception/type-mismatch")
        public void throwTypeMismatch(@RequestParam UUID id) {
        }

        @GetMapping("/test-exception/unexpected")
        public void throwUnexpectedException() {
            throw new NullPointerException("Simulated application panic context");
        }

        @GetMapping("/test-exception/constraint")
        public void throwConstraintViolationException() {
            ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
            Path path = Mockito.mock(Path.class);

            when(path.toString()).thenReturn("createEvent.price");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("Price cannot be negative");

            Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
            throw new ConstraintViolationException(violations);
        }
    }
}
