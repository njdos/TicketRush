package com.rush.ticket.catalog.integration.repository;

import com.rush.ticket.catalog.entity.OutboxEvent;
import com.rush.ticket.catalog.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OutboxEventRepositoryIT {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void findTop10ByStatusOrderByCreatedAtAsc_ReturnsOrderedAndLimitedEvents() {
        // Arrange
        String statusToFind = "PENDING";
        String statusToIgnore = "PROCESSED";

        for (int i = 1; i <= 12; i++) {
            OutboxEvent event = new OutboxEvent(
                    "aggregate-id-" + i,
                    "SeatsGenerated",
                    "{\"seats\": 100}"
            );
            event.setStatus(statusToFind);
            outboxEventRepository.save(event);
        }

        OutboxEvent ignoredEvent = new OutboxEvent(
                "ignored-id",
                "SeatsGenerated",
                "{\"seats\": 50}"
        );
        ignoredEvent.setStatus(statusToIgnore);
        outboxEventRepository.save(ignoredEvent);

        // Act
        List<OutboxEvent> result = outboxEventRepository.findTop10ByStatusOrderByCreatedAtAsc(statusToFind);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size());
        boolean containsProcessed = result.stream().anyMatch(e -> statusToIgnore.equals(e.getStatus()));
        assertFalse(containsProcessed);
    }
}
