package com.rush.ticket.catalog.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rush.ticket.catalog.dto.event.SeatsGeneratedEvent;
import com.rush.ticket.catalog.dto.req.EventRequestDto;
import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.entity.Event;
import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.exception.EventNotFoundException;
import com.rush.ticket.catalog.repository.EventRepository;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private UUID eventId;
    private UUID organizerId;
    private EventRequestDto requestDto;
    private Event mockEvent;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        organizerId = UUID.randomUUID();

        requestDto = new EventRequestDto(
                "Rock Festival",
                "Main Arena",
                LocalDateTime.now().plusDays(5),
                500,
                new BigDecimal("99.99")
        );

        mockEvent = spy(new Event(
                requestDto.name(),
                requestDto.venue(),
                requestDto.startsAt(),
                requestDto.totalSeats(),
                requestDto.price(),
                organizerId
        ));
    }

    @Test
    void createEvent_Success() throws JsonProcessingException {
        // Arrange
        when(mockEvent.getId()).thenReturn(eventId);
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);
        when(objectMapper.writeValueAsString(any(SeatsGeneratedEvent.class))).thenReturn("{\"mock\":\"json\"}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // Act
        EventResponseDto result = eventService.createEvent(requestDto, organizerId);

        // Assert
        assertNotNull(result);
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    // --- ADDED TO TARGET THE CATCH BLOCK EXTENSION ---
    @Test
    void createEvent_SerializationFailure_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        when(mockEvent.getId()).thenReturn(eventId);
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        // Force ObjectMapper to throw a concrete subclass of JsonProcessingException
        JsonProcessingException serializationException = InvalidFormatException.from(
                null, "Simulated serialization crash", new Object(), String.class
        );
        when(objectMapper.writeValueAsString(any(SeatsGeneratedEvent.class))).thenThrow(serializationException);

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            eventService.createEvent(requestDto, organizerId);
        });

        assertEquals("Event serialization failed", thrownException.getMessage());
        assertSame(serializationException, thrownException.getCause());

        // Verify that the event was saved, but the outbox save block was bypassed due to the crash
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void listEvents_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(mockEvent), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventResponseDto> result = eventService.listEvents(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(eventRepository, times(1)).findAll(pageable);
    }

    @Test
    void getEventById_Success() {
        // Arrange
        when(mockEvent.getId()).thenReturn(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        // Act
        EventResponseDto result = eventService.getEventById(eventId);

        // Assert
        assertNotNull(result);
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getEventById_ThrowsEventNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () -> eventService.getEventById(eventId));
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void updateEvent_Success() {
        // Arrange
        when(mockEvent.getId()).thenReturn(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        // Act
        EventResponseDto result = eventService.updateEvent(eventId, requestDto);

        // Assert
        assertNotNull(result);
        verify(mockEvent, times(1)).update(any(), any(), any(), any());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void updateEvent_ThrowsEventNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(eventId, requestDto));
        verify(eventRepository, times(1)).findById(eventId);
    }
}
