package com.rush.ticket.catalog.kafka;

import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class OutboxProcessor {

    private static final String TOPIC = "seats-generated-topic";
    private static final String METRIC_PUBLISH = "catalog.outbox.publish";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaOutboxPublisher publisher;
    private final MeterRegistry meterRegistry;
    private final int maxAttempts;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository,
                           KafkaOutboxPublisher publisher,
                           MeterRegistry meterRegistry,
                           @Value("${catalog.outbox.max-attempts:10}") int maxAttempts) {
        this.outboxEventRepository = outboxEventRepository;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        // Background job has no incoming request, so it gets its own correlation id per batch
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", "outbox-" + UUID.randomUUID().toString().substring(0, 8))) {
            log.debug("Found {} pending outbox events to process", pendingEvents.size());
            for (OutboxEvent event : pendingEvents) {
                try (MDC.MDCCloseable e = MDC.putCloseable("outboxEventId", String.valueOf(event.getId()));
                     MDC.MDCCloseable a = MDC.putCloseable("aggregateId", event.getAggregateId())) {
                    if (!publish(event)) {
                        return;
                    }
                }
            }
        }
    }

    /** @return true if the batch may continue, false if it must stop (broker problem). */
    private boolean publish(OutboxEvent event) {
        try {
            publisher.publish(TOPIC, event.getAggregateId(), event.getPayload());

            event.setStatus("SENT");
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
            count("success");
            log.info("Outbox event published to Kafka topic {}", TOPIC);
            return true;
        } catch (CallNotPermittedException ex) {
            // Circuit is open: the broker is known to be down. Events stay PENDING, nothing is lost.
            count("circuit_open");
            log.warn("Kafka circuit '{}' is open, skipping outbox batch", ex.getCausingCircuitBreakerName());
            return false;
        } catch (Exception ex) {
            count("failure");
            event.setAttempts(event.getAttempts() + 1);
            if (event.getAttempts() >= maxAttempts) {
                event.setStatus("FAILED");
                log.error("Outbox event marked FAILED after {} attempts", event.getAttempts(), ex);
            } else {
                log.warn("Failed to publish outbox event (attempt {}/{}): {}",
                        event.getAttempts(), maxAttempts, ex.getMessage());
            }
            outboxEventRepository.save(event);
            // Stop the batch: keeps per-aggregate ordering and avoids hammering a sick broker.
            return false;
        }
    }

    private void count(String result) {
        meterRegistry.counter(METRIC_PUBLISH, "result", result).increment();
    }
}
