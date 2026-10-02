package com.rush.ticket.reservation.service;

import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.exception.SeatAlreadyReservedException;
import com.rush.ticket.reservation.lock.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private static final Duration HOLD_TTL = Duration.ofMinutes(10);

    private final SeatLockService lockService;
    private final ReservationWriter writer;

    public ReservationService(SeatLockService lockService, ReservationWriter writer) {
        this.lockService = lockService;
        this.writer = writer;
    }

    public Reservation reserveSeat(UUID eventId, UUID seatId, UUID userId) {
        String token = UUID.randomUUID().toString();
        boolean locked = lockService.tryLock(eventId.toString(), seatId.toString(), token, HOLD_TTL);

        if (!locked) {
            throw new SeatAlreadyReservedException(seatId.toString());
        }

        try {
            Instant expiresAt = Instant.now().plus(HOLD_TTL);
            Reservation reservation = writer.holdSeat(seatId, eventId, userId, expiresAt);
            log.info("Seat {} held by user {}, reservation {}", seatId, userId, reservation.getId());
            return reservation;
        } catch (RuntimeException e) {
            // DB-запис не вдався (наприклад, @Version конфлікт) — відкочуємо Redis-лок,
            // інакше місце буде "мертво" заблоковане на 10 хв без відповідної броні.
            lockService.unlock(eventId.toString(), seatId.toString(), token);
            throw e;
        }
    }

    public void cancelReservation(UUID reservationId, UUID eventId, UUID seatId) {
        writer.releaseSeat(reservationId);
        lockService.unlock(eventId.toString(), seatId.toString(), null);
        // null тут навмисно спрощено: оскільки TTL все одно звільнить ключ,
        // а Lua-скрипт просто не видалить чужий токен. В MVP прийнятно,
        // у фазі з Kafka зберігатимемо токен у Reservation для точного unlock.
    }
}