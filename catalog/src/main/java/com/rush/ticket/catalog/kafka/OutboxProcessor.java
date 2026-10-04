package com.rush.ticket.catalog.kafka;

import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "seats-generated-topic";

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to process", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            String messageKey = event.getAggregateId();

            try {
                kafkaTemplate.send(TOPIC, messageKey, event.getPayload()).get();

                event.setStatus("SENT");
                event.setProcessedAt(Instant.now());
                log.info("Outbox event {} successfully published to Kafka topic {}", event.getId(), TOPIC);
            } catch (Exception ex) {
                event.setStatus("FAILED");
                log.error("Failed to publish outbox event {} due to broker failure", event.getId(), ex);
            }

            outboxEventRepository.save(event);
        }
    }

}
