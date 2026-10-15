package com.rush.ticket.catalog.kafka;

/** Infrastructure failure while publishing to Kafka; counted by the circuit breaker and retried. */
public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
