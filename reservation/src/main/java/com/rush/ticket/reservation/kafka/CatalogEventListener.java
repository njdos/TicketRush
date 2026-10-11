package com.rush.ticket.reservation.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogEventListener {

    private final SeatRepository seatRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "seats-generated-topic", groupId = "reservation-group")
    @Transactional
    public void handleSeatsGenerated(String messagePayload) {
        try {
            SeatsGeneratedEvent event = objectMapper.readValue(messagePayload, SeatsGeneratedEvent.class);

            String idempotencyKey = "processed_event:" + event.eventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSED", Duration.ofDays(1));
            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Duplicate SeatsGeneratedEvent ignored: {}", event.eventId());
                return;
            }

            if (event.totalSeats() != null && event.totalSeats() > 0) {
                for (int i = 0; i < event.totalSeats(); i++) {
                    seatRepository.save(new Seat(UUID.randomUUID(), event.concertId()));
                }
                log.info("Successfully generated and persisted {} seats for concert: {}", event.totalSeats(), event.concertName());
            } else {
                log.warn("Received event but totalSeats count is null or 0 for concert: {}", event.concertId());
            }

        } catch (Exception e) {
            log.error("Failed to parse SeatsGeneratedEvent JSON from Kafka. Payload: {}", messagePayload, e);
        }
    }
}