package com.rush.ticket.reservation.service;

import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.entity.Seat;
import com.rush.ticket.reservation.repository.ReservationRepository;
import com.rush.ticket.reservation.repository.SeatRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
public class ReservationWriter {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    public ReservationWriter(SeatRepository seatRepository, ReservationRepository reservationRepository) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation holdSeat(UUID seatId, UUID eventId, UUID userId, Instant expiresAt) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NoSuchElementException("Seat not found: " + seatId));
        seat.markHeld();          // кине IllegalStateException, якщо вже не FREE
        seatRepository.save(seat); // @Version спрацює тут як страховка

        Reservation reservation = new Reservation(userId, seatId, eventId, expiresAt);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseSeat(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + reservationId));
        reservation.release();

        Seat seat = seatRepository.findById(reservation.getSeatId()).orElseThrow();
        seat.markFree();
        seatRepository.save(seat);
    }
}