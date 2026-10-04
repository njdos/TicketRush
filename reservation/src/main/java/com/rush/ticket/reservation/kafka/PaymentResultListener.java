package com.rush.ticket.reservation.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultListener {

    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-results-topic", groupId = "reservation-saga-group")
    @Transactional
    public void handlePaymentResult(String messagePayload) throws Exception {
        PaymentResultPayload result = objectMapper.readValue(messagePayload, PaymentResultPayload.class);

        // Inbox-ідемпотентність: той самий eventId Kafka-повідомлення обробляємо один раз
        String idempotencyKey = "processed_payment_event:" + result.eventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", Duration.ofDays(1));
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Duplicate PaymentResult event ignored: {}", result.eventId());
            return;
        }

        if (result.success()) {
            confirmReservation(result.reservationId());
        } else {
            compensateFailedReservation(result.reservationId());
        }
    }

    private void confirmReservation(UUID reservationId) {
        reservationRepository.findById(reservationId).ifPresentOrElse(res -> {
            res.confirm(); // dirty checking збереже зміну без явного save()
            log.info("SAGA SUCCESS: Reservation {} CONFIRMED", reservationId);
        }, () -> {
            throw new NoSuchElementException("Reservation not found: " + reservationId);
        });
    }

    private void compensateFailedReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("Reservation not found: " + reservationId));
        reservation.release();

        // Явний unlock токеном тут неможливий (токен не зберігається) — TTL прибере ключ сам,
        // як і в cancelReservation(). Якщо потрібна миттєва реакція — додати поле lockToken в Reservation.
        log.warn("SAGA COMPENSATED: Payment failed for reservation {}, seat released", reservationId);
    }

    private record PaymentResultPayload(UUID eventId, UUID reservationId, boolean success) {}
}