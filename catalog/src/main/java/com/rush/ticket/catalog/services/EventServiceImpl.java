package com.rush.ticket.catalog.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rush.ticket.catalog.dto.req.EventRequestDto;
import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.dto.event.SeatsGeneratedEvent;
import com.rush.ticket.catalog.entity.Event;
import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.exception.EventNotFoundException;
import com.rush.ticket.catalog.mapper.EventMapper;
import com.rush.ticket.catalog.repository.EventRepository;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper; // Для маппинга DTO в JSON-строку

    public EventServiceImpl(EventRepository eventRepository,
                            OutboxEventRepository outboxEventRepository,
                            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public EventResponseDto createEvent(EventRequestDto request, UUID organizerId) {
        Event event = new Event(
                request.name(),
                request.venue(),
                request.startsAt(),
                request.totalSeats(),
                request.price(),
                organizerId
        );
        Event saved = eventRepository.save(event);
        log.info("Created event {} by organizer {}", saved.getId(), organizerId);

        SeatsGeneratedEvent eventDto = new SeatsGeneratedEvent(
                UUID.randomUUID(),
                saved.getId(),
                saved.getName(),
                saved.getTotalSeats(),
                saved.getPrice(),
                LocalDateTime.now()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(eventDto);
            OutboxEvent outboxEvent = new OutboxEvent(
                    saved.getId().toString(),
                    "SeatsGenerated",
                    jsonPayload
            );
            outboxEventRepository.save(outboxEvent);
            log.info("Saved SeatsGenerated event to outbox for event {}", saved.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SeatsGeneratedEvent for event {}", saved.getId(), e);
            throw new RuntimeException("Event serialization failed", e);
        }

        return EventMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponseDto> listEvents(Pageable pageable) {
        return eventRepository.findAll(pageable).map(EventMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return EventMapper.toResponseDto(event);
    }

    @Override
    @Transactional
    public EventResponseDto updateEvent(UUID id, EventRequestDto request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        event.update(request.name(), request.venue(), request.startsAt(), request.price());
        log.info("Updated event {}", id);
        return EventMapper.toResponseDto(event);
    }
}
