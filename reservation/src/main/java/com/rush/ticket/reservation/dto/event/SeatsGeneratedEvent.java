package com.rush.ticket.reservation.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SeatsGeneratedEvent(
        UUID eventId,
        UUID concertId,
        String concertName,
        Integer totalSeats,
        BigDecimal price,
        LocalDateTime occurredAt
) {
}