package com.rush.ticket.reservation.dto.reqResp;

import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.entity.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponseDto(
        UUID id, UUID seatId, UUID eventId, ReservationStatus status, Instant expiresAt
) {
    public static ReservationResponseDto from(Reservation r) {
        return new ReservationResponseDto(r.getId(), r.getSeatId(), r.getEventId(), r.getStatus(), r.getExpiresAt());
    }
}