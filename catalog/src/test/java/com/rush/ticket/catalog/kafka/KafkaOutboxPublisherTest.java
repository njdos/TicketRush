package com.rush.ticket.catalog.kafka;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Plain unit tests: the decorators are built programmatically, no Spring context needed. */
@ExtendWith(MockitoExtension.class)
class KafkaOutboxPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publish_WhenSendTimesOut_ThrowsKafkaPublishException() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<SendResult<String, String>>()); // never completes
        KafkaOutboxPublisher publisher = new KafkaOutboxPublisher(kafkaTemplate, Duration.ofMillis(50));

        assertThatThrownBy(() -> publisher.publish("t", "k", "v"))
                .isInstanceOf(KafkaPublishException.class);
    }

    @Test
    void publish_WhenBrokerKeepsFailing_CircuitOpensAndStopsCallingKafka() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);
        KafkaOutboxPublisher publisher = new KafkaOutboxPublisher(kafkaTemplate, Duration.ofSeconds(1));

        CircuitBreaker cb = CircuitBreaker.of("kafkaPublisher", CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .recordExceptions(KafkaPublishException.class)
                .build());
        Retry retry = Retry.of("kafkaPublisher", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .retryExceptions(KafkaPublishException.class)
                .build());

        // Same order as the annotations: Retry( CircuitBreaker( publish ) )
        Runnable decorated = Retry.decorateRunnable(retry,
                CircuitBreaker.decorateRunnable(cb, () -> publisher.publish("t", "k", "v")));

        assertThatThrownBy(decorated::run).isInstanceOf(KafkaPublishException.class); // 2 calls
        assertThatThrownBy(decorated::run).isInstanceOf(KafkaPublishException.class); // 2 calls -> window full
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(decorated::run).isInstanceOf(CallNotPermittedException.class);
        verify(kafkaTemplate, times(4)).send(anyString(), anyString(), anyString());
    }
}
