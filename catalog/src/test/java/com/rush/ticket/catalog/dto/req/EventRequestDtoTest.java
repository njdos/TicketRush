package com.rush.ticket.catalog.dto.req;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EventRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsAreValid_thenNoValidationViolations() {
        EventRequestDto dto = new EventRequestDto(
                "Rock Concert",
                "Madison Square Garden",
                LocalDateTime.now().plusDays(2),
                100,
                new BigDecimal("49.99")
        );

        Set<ConstraintViolation<EventRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenBlankFields_thenValidationFails() {
        EventRequestDto dto = new EventRequestDto(
                "",
                "   ",
                LocalDateTime.now().plusDays(1),
                50,
                BigDecimal.TEN
        );

        Set<ConstraintViolation<EventRequestDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size());

        boolean hasNameError = violations.stream().anyMatch(v -> v.getMessage().equals("Event name cannot be blank"));
        boolean hasVenueError = violations.stream().anyMatch(v -> v.getMessage().equals("Venue cannot be blank"));

        assertTrue(hasNameError);
        assertTrue(hasVenueError);
    }

    @Test
    void whenPastDateAndInvalidNumbers_thenValidationFails() {
        EventRequestDto dto = new EventRequestDto(
                "Valid Name",
                "Valid Venue",
                LocalDateTime.now().minusDays(1),
                0,
                new BigDecimal("-5.00")
        );

        Set<ConstraintViolation<EventRequestDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(3, violations.size());

        boolean hasDateError = violations.stream().anyMatch(v -> v.getMessage().equals("Event must start in the future"));
        boolean hasSeatsError = violations.stream().anyMatch(v -> v.getMessage().equals("Total seats must be at least 1"));
        boolean hasPriceError = violations.stream().anyMatch(v -> v.getMessage().equals("Price cannot be negative"));

        assertTrue(hasDateError);
        assertTrue(hasSeatsError);
        assertTrue(hasPriceError);
    }
}
