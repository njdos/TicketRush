package com.rush.ticket.catalog.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequestDto(
        @NotBlank(message = "Event name cannot be blank")
        String name,

        @NotBlank(message = "Venue cannot be blank")
        String venue,

        @NotNull(message = "Start date and time is required")
        @Future(message = "Event must start in the future")
        LocalDateTime startsAt,

        @NotNull(message = "Total seats count is required")
        @Min(value = 1, message = "Total seats must be at least 1")
        Integer totalSeats,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        BigDecimal price
) {}
