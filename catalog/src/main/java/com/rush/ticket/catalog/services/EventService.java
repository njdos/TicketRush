package com.rush.ticket.catalog.services;

import com.rush.ticket.catalog.dto.reqResp.EventRequestDto;
import com.rush.ticket.catalog.dto.reqResp.EventResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {
    EventResponseDto createEvent(EventRequestDto request, UUID organizerId);
    Page<EventResponseDto> listEvents(Pageable pageable);
    EventResponseDto getEventById(UUID id);
    EventResponseDto updateEvent(UUID id, EventRequestDto request);
}