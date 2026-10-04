package com.rush.ticket.catalog.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SeatsGeneratedEvent(
        UUID eventId,
        UUID concertId,
        String concertName,
        Integer totalSeats,
        BigDecimal price,
        LocalDateTime occurredAt
) {}
