package com.rush.ticket.catalog.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponseDto(
        UUID id,
        String name,
        String venue,
        LocalDateTime startsAt,
        Integer totalSeats,
        BigDecimal price,
        UUID organizerId,
        Instant createdAt
) {
}
