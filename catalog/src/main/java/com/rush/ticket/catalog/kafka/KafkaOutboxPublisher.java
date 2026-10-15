package com.rush.ticket.catalog.kafka;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Single place where we talk to Kafka. It is a separate bean on purpose:
 * Resilience4j annotations work through Spring AOP proxies and are ignored on self-invocation.
 * Order: Retry( CircuitBreaker( send ) ). When the circuit is open, CallNotPermittedException
 * is thrown immediately and is not retried (it is not in retry-exceptions).
 */
@Component
public class KafkaOutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration sendTimeout;

    public KafkaOutboxPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                @Value("${catalog.outbox.send-timeout:8s}") Duration sendTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeout = sendTimeout;
    }

    @Retry(name = "kafkaPublisher")
    @CircuitBreaker(name = "kafkaPublisher")
    public void publish(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaPublishException("Failed to publish to " + topic, e);
        }
    }
}
