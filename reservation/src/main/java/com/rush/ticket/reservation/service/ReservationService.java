package com.rush.ticket.reservation.service;

import com.rush.ticket.reservation.entity.Reservation;
import java.util.UUID;

public interface ReservationService {
    Reservation reserveSeat(UUID eventId, UUID seatId, UUID userId);
    void cancelReservation(UUID reservationId);
    void requestPayment(UUID reservationId);
}