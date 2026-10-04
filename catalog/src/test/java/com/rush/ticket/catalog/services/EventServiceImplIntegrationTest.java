package com.rush.ticket.catalog.services;

import com.rush.ticket.catalog.dto.reqResp.EventRequestDto;
import com.rush.ticket.catalog.dto.reqResp.EventResponseDto;
import com.rush.ticket.catalog.entity.Event;
import com.rush.ticket.catalog.exception.EventNotFoundException;
import com.rush.ticket.catalog.integration.IntegrationTestBase;
import com.rush.ticket.catalog.kafka.OutboxProcessor;
import com.rush.ticket.catalog.repository.EventRepository;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventServiceImplIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EventService eventService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldCreateEventInDatabase() {
        // Arrange
        UUID organizerId = UUID.randomUUID();
        EventRequestDto request = new EventRequestDto(
                "Integration Fest", "Docker Container", LocalDateTime.now().plusDays(1), 100, new BigDecimal("10.00")
        );

        // Act
        EventResponseDto response = eventService.createEvent(request, organizerId);

        // Assert
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Integration Fest");
        assertThat(response.organizerId()).isEqualTo(organizerId);
        assertThat(eventRepository.findById(response.id())).isPresent();
    }

    @Test
    void shouldReturnPagedEventsFromDatabase() {
        // Arrange
        Event event1 = new Event("Event 1", "Venue 1", LocalDateTime.now().plusDays(1), 50, new BigDecimal("15.00"), UUID.randomUUID());
        Event event2 = new Event("Event 2", "Venue 2", LocalDateTime.now().plusDays(2), 60, new BigDecimal("20.00"), UUID.randomUUID());
        eventRepository.saveAll(List.of(event1, event2));

        // Act
        Page<EventResponseDto> page = eventService.listEvents(PageRequest.of(0, 10));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(EventResponseDto::name).containsExactlyInAnyOrder("Event 1", "Event 2");
    }

    @Test
    void shouldGetEventById() {
        // Arrange
        Event event = new Event("Single Event", "Hall", LocalDateTime.now().plusDays(3), 80, new BigDecimal("35.00"), UUID.randomUUID());
        Event saved = eventRepository.save(event);

        // Act
        EventResponseDto found = eventService.getEventById(saved.getId());

        // Assert
        assertThat(found.id()).isEqualTo(saved.getId());
        assertThat(found.name()).isEqualTo("Single Event");
    }

    @Test
    void shouldThrowExceptionWhenEventNotFoundById() {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act & Assert
        assertThatThrownBy(() -> eventService.getEventById(randomId))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void shouldUpdateExistingEventInDatabase() {
        // Arrange
        Event event = new Event("Old Title", "Old Venue", LocalDateTime.now().plusDays(4), 120, new BigDecimal("45.00"), UUID.randomUUID());
        Event saved = eventRepository.save(event);

        EventRequestDto updateRequest = new EventRequestDto(
                "New Title", "New Venue", LocalDateTime.now().plusDays(5), 120, new BigDecimal("55.00")
        );

        // Act
        EventResponseDto updatedResponse = eventService.updateEvent(saved.getId(), updateRequest);

        // Assert
        assertThat(updatedResponse.name()).isEqualTo("New Title");
        assertThat(updatedResponse.venue()).isEqualTo("New Venue");
        assertThat(updatedResponse.price()).isEqualByComparingTo("55.00");

        // Дополнительно проверяем, что в БД данные обновились
        Event databaseEvent = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(databaseEvent.getName()).isEqualTo("New Title");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingEvent() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        EventRequestDto updateRequest = new EventRequestDto(
                "Title", "Venue", LocalDateTime.now().plusDays(5), 100, new BigDecimal("50.00")
        );

        // Act & Assert
        assertThatThrownBy(() -> eventService.updateEvent(randomId, updateRequest))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void shouldCreateEventAndSaveItToOutboxTableTable() {
        // Arrange
        UUID organizerId = UUID.randomUUID();
        EventRequestDto request = new EventRequestDto(
                "Outbox Fest", "Postgres Room", LocalDateTime.now().plusDays(1), 500, new BigDecimal("99.99")
        );

        // Act
        EventResponseDto response = eventService.createEvent(request, organizerId);

        // Assert
        assertThat(response.id()).isNotNull();

        // 🔥 Проверяем, что в рамках одной транзакции запись попала в Outbox в статусе PENDING
        var outboxEvents = outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");
        assertThat(outboxEvents).isNotEmpty();

        var targetEvent = outboxEvents.stream()
                .filter(e -> e.getAggregateId().equals(response.id().toString()))
                .findFirst();

        assertThat(targetEvent).isPresent();
        assertThat(targetEvent.get().getEventType()).isEqualTo("SeatsGenerated");
        assertThat(targetEvent.get().getPayload()).contains("Outbox Fest");
    }

    @Test
    void shouldSuccessfullyProcessOutboxEventAndChangeStatusToSent() {
        // Arrange
        UUID organizerId = UUID.randomUUID();
        EventRequestDto request = new EventRequestDto(
                "Async Dynamic Show", "Kafka Hall", LocalDateTime.now().plusDays(2), 250, new BigDecimal("45.00")
        );

        // Act
        EventResponseDto response = eventService.createEvent(request, organizerId);

        outboxProcessor.processPendingOutboxEvents();

        // Assert
        var events = outboxEventRepository.findAll();
        var processedEvent = events.stream()
                .filter(e -> e.getAggregateId().equals(response.id().toString()))
                .findFirst()
                .orElseThrow();

        assertThat(processedEvent.getStatus()).isEqualTo("SENT");
        assertThat(processedEvent.getProcessedAt()).isNotNull();
    }

}
