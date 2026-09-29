package com.rush.ticket.catalog.mapper;

import com.rush.ticket.catalog.dtos.EventResponseDto;
import com.rush.ticket.catalog.entity.Event;

public final class EventMapper {

    private EventMapper() {}

    public static EventResponseDto toResponseDto(Event event) {
        return new EventResponseDto(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getStartsAt(),
                event.getTotalSeats(),
                event.getPrice(),
                event.getOrganizerId(),
                event.getCreatedAt());
    }
}