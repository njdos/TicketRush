package com.rush.ticket.reservation.kafka;

import com.rush.ticket.reservation.dto.event.SeatsGeneratedEvent;
import com.rush.ticket.reservation.entity.Seat;
import com.rush.ticket.reservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogEventListener {

    private final SeatRepository seatRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String TOPIC = "seats-generated-topic";

    @KafkaListener(topics = TOPIC, groupId = "reservation-group")
    @Transactional
    public void handleSeatsGenerated(SeatsGeneratedEvent event) {
        String idempotencyKey = "processed_event:" + event.eventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", Duration.ofDays(1));
        if (Boolean.FALSE.equals(isNew)) {
            log.warn("Duplicate SeatsGeneratedEvent ignored: {}", event.eventId());
            return;
        }

        for (var seatId : event.seatIds()) {
            seatRepository.save(new Seat(seatId, event.concertId()));
        }

        log.info("Persisted {} seats for concert {}", event.seatIds().size(), event.concertId());
    }
}