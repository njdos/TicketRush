package com.rush.ticket.catalog.mapper;

import com.rush.ticket.catalog.dto.resp.EventResponseDto;
import com.rush.ticket.catalog.entity.Event;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    @Test
    void toResponseDto_ShouldMapAllFieldsCorrectly() {
        // Arrange
        UUID expectedId = UUID.randomUUID();
        UUID expectedOrganizerId = UUID.randomUUID();
        String expectedName = "Rock Concert";
        String expectedVenue = "Main Arena";
        LocalDateTime expectedStartsAt = LocalDateTime.now().plusDays(2);
        int expectedTotalSeats = 150;
        BigDecimal expectedPrice = new BigDecimal("75.50");

        Event event = new Event(
                expectedName,
                expectedVenue,
                expectedStartsAt,
                expectedTotalSeats,
                expectedPrice,
                expectedOrganizerId
        );

        // Act
        EventResponseDto responseDto = EventMapper.toResponseDto(event);

        // Assert
        assertNotNull(responseDto);
        assertEquals(event.getId(), responseDto.id());
        assertEquals(expectedName, responseDto.name());
        assertEquals(expectedVenue, responseDto.venue());
        assertEquals(expectedStartsAt, responseDto.startsAt());
        assertEquals(expectedTotalSeats, responseDto.totalSeats());
        assertEquals(expectedPrice, responseDto.price());
        assertEquals(expectedOrganizerId, responseDto.organizerId());
        assertEquals(event.getCreatedAt(), responseDto.createdAt());
    }
}
