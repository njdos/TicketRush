package com.rush.ticket.catalog.kafka;

import com.rush.ticket.catalog.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Outbox health as metrics. The most important SLI of the pattern is the lag of the oldest PENDING event.
 * <p>
 * Values are refreshed by a scheduled job, NOT computed inside the gauge callback: a gauge is read on every
 * Prometheus scrape, and a DB query there would make /actuator/prometheus slow or failing exactly when the DB is sick.
 */
@Component
@Slf4j
public class OutboxMetrics {

    private final OutboxEventRepository repository;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();

    public OutboxMetrics(OutboxEventRepository repository, MeterRegistry registry) {
        this.repository = repository;
        Gauge.builder("catalog.outbox.events", pending, AtomicLong::get)
                .tag("status", "PENDING")
                .description("Outbox events waiting to be published")
                .register(registry);
        Gauge.builder("catalog.outbox.events", failed, AtomicLong::get)
                .tag("status", "FAILED")
                .description("Outbox events that exhausted all attempts and need manual attention")
                .register(registry);
        Gauge.builder("catalog.outbox.oldest.pending.age", oldestPendingAgeSeconds, AtomicLong::get)
                .baseUnit("seconds")
                .description("Age of the oldest PENDING outbox event (publishing lag)")
                .register(registry);
    }

    @Scheduled(fixedDelay = 15_000)
    public void refresh() {
        try {
            pending.set(repository.countByStatus("PENDING"));
            failed.set(repository.countByStatus("FAILED"));
            oldestPendingAgeSeconds.set(repository.findFirstByStatusOrderByCreatedAtAsc("PENDING")
                    .map(e -> Duration.between(e.getCreatedAt(), Instant.now()).toSeconds())
                    .orElse(0L));
        } catch (Exception e) {
            // keep the last known values; the DB problem is already visible via circuit breaker / Hikari metrics
            log.warn("Could not refresh outbox metrics: {}", e.toString());
        }
    }
}
