package com.rush.ticket.catalog.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Logs circuit state transitions and retries, so incidents are visible in logs, not only in metrics. */
@Configuration
@Slf4j
public class ResilienceEventLoggingConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                event.getAddedEntry().getEventPublisher()
                        .onStateTransition(e -> log.warn("CircuitBreaker '{}' {}",
                                e.getCircuitBreakerName(), e.getStateTransition()))
                        .onCallNotPermitted(e -> log.debug("CircuitBreaker '{}' rejected a call",
                                e.getCircuitBreakerName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
                // nothing to do
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
                // nothing to do
            }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> retryLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                event.getAddedEntry().getEventPublisher()
                        .onRetry(e -> log.warn("Retry '{}' attempt #{} after: {}",
                                e.getName(), e.getNumberOfRetryAttempts(),
                                e.getLastThrowable() == null ? "n/a" : e.getLastThrowable().toString()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
                // nothing to do
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
                // nothing to do
            }
        };
    }
}
