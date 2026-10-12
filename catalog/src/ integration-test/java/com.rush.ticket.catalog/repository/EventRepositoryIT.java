package com.rush.ticket.catalog.repository;

import com.rush.ticket.catalog.entity.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryIT {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void findAll_ReturnsPagedEvents() {
        UUID organizerId = UUID.randomUUID();

        Event event1 = new Event(
                "Rock Festival",
                "Arena A",
                LocalDateTime.now().plusDays(2),
                500,
                new BigDecimal("99.99"),
                organizerId
        );

        Event event2 = new Event(
                "Pop Concert",
                "Arena B",
                LocalDateTime.now().plusDays(5),
                300,
                new BigDecimal("49.99"),
                organizerId
        );

        eventRepository.save(event1);
        eventRepository.save(event2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> result = eventRepository.findAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }
}
