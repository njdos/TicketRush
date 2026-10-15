package com.rush.ticket.catalog.kafka;

import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaOutboxPublisher publisher;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private OutboxProcessor outboxProcessor;
    private OutboxEvent sampleEvent;
    private OutboxEvent secondEvent;

    @BeforeEach
    void setUp() {
        outboxProcessor = new OutboxProcessor(outboxEventRepository, publisher, meterRegistry, MAX_ATTEMPTS);

        sampleEvent = newEvent("agg-123");
        secondEvent = newEvent("agg-456");
    }

    private OutboxEvent newEvent(String aggregateId) {
        OutboxEvent e = new OutboxEvent();
        e.setId(UUID.randomUUID());
        e.setAggregateId(aggregateId);
        e.setPayload("{\"seats\": 100}");
        e.setStatus("PENDING");
        return e;
    }

    @Test
    void processPendingOutboxEvents_WhenNoEvents_ReturnsImmediately() {
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Collections.emptyList());

        outboxProcessor.processPendingOutboxEvents();

        verifyNoInteractions(publisher);
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void processPendingOutboxEvents_WhenEventsExist_PublishesSuccessfully() {
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Collections.singletonList(sampleEvent));

        outboxProcessor.processPendingOutboxEvents();

        assertEquals("SENT", sampleEvent.getStatus());
        assertNotNull(sampleEvent.getProcessedAt());
        verify(publisher).publish(eq("seats-generated-topic"), eq("agg-123"), eq("{\"seats\": 100}"));
        verify(outboxEventRepository).save(sampleEvent);
        assertEquals(1.0, result("success"));
    }

    @Test
    void processPendingOutboxEvents_WhenCircuitOpen_KeepsEventsPendingAndStopsBatch() {
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(sampleEvent, secondEvent));
        CircuitBreaker cb = CircuitBreaker.ofDefaults("kafkaPublisher");
        doThrow(CallNotPermittedException.createCallNotPermittedException(cb))
                .when(publisher).publish(anyString(), anyString(), anyString());

        outboxProcessor.processPendingOutboxEvents();

        assertEquals("PENDING", sampleEvent.getStatus());
        assertEquals(0, sampleEvent.getAttempts());
        verify(publisher, times(1)).publish(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        assertEquals(1.0, result("circuit_open"));
    }

    @Test
    void processPendingOutboxEvents_WhenPublishFails_IncrementsAttemptsAndStopsBatch() {
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(sampleEvent, secondEvent));
        doThrow(new KafkaPublishException("boom", new RuntimeException()))
                .when(publisher).publish(anyString(), anyString(), anyString());

        outboxProcessor.processPendingOutboxEvents();

        assertEquals("PENDING", sampleEvent.getStatus());
        assertEquals(1, sampleEvent.getAttempts());
        assertEquals("PENDING", secondEvent.getStatus());
        verify(publisher, times(1)).publish(anyString(), anyString(), anyString());
        verify(outboxEventRepository).save(sampleEvent);
        assertEquals(1.0, result("failure"));
    }

    @Test
    void processPendingOutboxEvents_WhenAttemptsExhausted_MarksFailed() {
        sampleEvent.setAttempts(MAX_ATTEMPTS - 1);
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Collections.singletonList(sampleEvent));
        doThrow(new KafkaPublishException("boom", new RuntimeException()))
                .when(publisher).publish(anyString(), anyString(), anyString());

        outboxProcessor.processPendingOutboxEvents();

        assertEquals("FAILED", sampleEvent.getStatus());
        assertEquals(MAX_ATTEMPTS, sampleEvent.getAttempts());
        verify(outboxEventRepository).save(sampleEvent);
    }

    private double result(String result) {
        return meterRegistry.counter("catalog.outbox.publish", "result", result).count();
    }
}
