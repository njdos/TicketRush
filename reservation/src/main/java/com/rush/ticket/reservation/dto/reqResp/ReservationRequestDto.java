package com.rush.ticket.reservation.dto.reqResp;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReservationRequestDto(
        @NotNull UUID eventId,
        @NotNull UUID seatId
) {}