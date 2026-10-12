package com.rush.ticket.catalog.kafka;

import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    private OutboxEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new OutboxEvent();
        sampleEvent.setId(UUID.randomUUID());
        sampleEvent.setAggregateId("agg-123");
        sampleEvent.setPayload("{\"seats\": 100}");
        sampleEvent.setStatus("PENDING");
    }

    @Test
    void processPendingOutboxEvents_WhenNoEvents_ReturnsImmediately() {
        // Arrange: Repository returns an empty list
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(Collections.emptyList());

        // Act
        outboxProcessor.processPendingOutboxEvents();

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void processPendingOutboxEvents_WhenEventsExist_PublishesSuccessfully() {
        // Arrange
        List<OutboxEvent> eventsList = Collections.singletonList(sampleEvent);
        when(outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(eventsList);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(eq("seats-generated-topic"), eq("agg-123"), eq("{\"seats\": 100}")))
                .thenReturn(future);

        // Act
        outboxProcessor.processPendingOutboxEvents();

        // Assert
        assertEquals("SENT", sampleEvent.getStatus());
        assertNotNull(sampleEvent.getProcessedAt());

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
        verify(outboxEventRepository, times(1)).save(sampleEvent);
    }

}
