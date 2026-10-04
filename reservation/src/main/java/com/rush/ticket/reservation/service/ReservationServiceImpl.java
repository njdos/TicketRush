package com.rush.ticket.reservation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rush.ticket.reservation.dto.event.PaymentRequestedEvent;
import com.rush.ticket.reservation.entity.OutboxEvent;
import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.exception.SeatAlreadyReservedException;
import com.rush.ticket.reservation.lock.SeatLockService;
import com.rush.ticket.reservation.repository.OutboxEventRepository;
import com.rush.ticket.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private static final Duration HOLD_TTL = Duration.ofMinutes(10);

    private final SeatLockService lockService;
    private final ReservationWriter writer;
    private final ReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
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
            lockService.unlock(eventId.toString(), seatId.toString(), token);
            throw e;
        }
    }

    @Override
    public void cancelReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + reservationId));
        writer.releaseSeat(reservationId);
        // token невідомий на цьому етапі — покладаємось на TTL (свідомий trade-off, обговорений раніше)
        lockService.unlock(reservation.getEventId().toString(), reservation.getSeatId().toString(), null);
    }

    @Override
    @Transactional
    public void requestPayment(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + reservationId));

        PaymentRequestedEvent paymentEvent = new PaymentRequestedEvent(
                reservation.getId(),
                reservation.getUserId(),
                null // TODO: ціна має братись з Seat/Event, зараз в доменній моделі немає price — додати поле при потребі
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(paymentEvent);
            OutboxEvent outboxEvent = new OutboxEvent(
                    reservation.getId().toString(),
                    "PaymentRequested",
                    jsonPayload
            );
            outboxEventRepository.save(outboxEvent);
            log.info("Reservation {} committed alongside its Outbox payment event", reservation.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentRequestedEvent for reservation: {}", reservation.getId(), e);
            throw new RuntimeException("Outbox serialization failed", e);
        }
    }
}