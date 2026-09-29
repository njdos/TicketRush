package com.rush.ticket.catalog.services;

import com.rush.ticket.catalog.dtos.EventRequestDto;
import com.rush.ticket.catalog.dtos.EventResponseDto;
import com.rush.ticket.catalog.entity.Event;
import com.rush.ticket.catalog.exceptions.EventNotFoundException;
import com.rush.ticket.catalog.mapper.EventMapper;
import com.rush.ticket.catalog.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;

    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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

        // TODO week 3: publish SeatsGenerated via Outbox after Reservation Service exists
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