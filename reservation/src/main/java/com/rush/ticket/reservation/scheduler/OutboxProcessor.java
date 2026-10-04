package com.rush.ticket.reservation.scheduler;

import com.rush.ticket.reservation.entity.OutboxEvent;
import com.rush.ticket.reservation.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "payment-requests-topic";

    @Scheduled(fixedDelay = 1000) // Execute background sweeps every 1 second
    @Transactional
    public void processPendingBillingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            String kafkaMessageKey = event.getAggregateId(); // Using reservationId as the message routing partition key [06.09.2026 20:41]

            kafkaTemplate.send(TOPIC, kafkaMessageKey, event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            updateEventStatus(event.getId(), "SENT");
                            log.info("Billing record outbox reference {} safely streamed onto Kafka topic {}", event.getId(), TOPIC);
                        } else {
                            updateEventStatus(event.getId(), "FAILED");
                            log.error("Outbox broker push dropped execution block for event: {}", event.getId(), ex);
                        }
                    });
        }
    }

    @Transactional
    public void updateEventStatus(UUID id, String status) {
        outboxEventRepository.findById(id).ifPresent(event -> {
            event.setStatus(status);
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
        });
    }
}
