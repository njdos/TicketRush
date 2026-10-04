package com.rush.ticket.reservation.dto.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID reservationId,
        UUID userId,
        BigDecimal totalAmount
) {}
