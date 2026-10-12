package com.rush.ticket.catalog.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        // Covers the default empty constructor
        Event event = new Event();
        UUID id = UUID.randomUUID();

        event.setId(id);
        assertEquals(id, event.getId());
    }

    @Test
    void testAllArgsConstructorAndCustomUpdate() {
        // Arrange
        UUID organizerId = UUID.randomUUID();
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);

        // Covers full constructor execution
        Event event = new Event(
                "Rock Show",
                "Theater A",
                startsAt,
                200,
                new BigDecimal("50.00"),
                organizerId
        );

        // Assert initial constructor mappings
        assertEquals("Rock Show", event.getName());
        assertEquals("Theater A", event.getVenue());

        // Act: Test custom update business method
        LocalDateTime newStartsAt = startsAt.plusDays(1);
        BigDecimal newPrice = new BigDecimal("60.00");
        event.update("Pop Show", "Theater B", newStartsAt, newPrice);

        // Assert updated values
        assertEquals("Pop Show", event.getName());
        assertEquals("Theater B", event.getVenue());
        assertEquals(newStartsAt, event.getStartsAt());
        assertEquals(newPrice, event.getPrice());
    }

    @Test
    void onCreate_ShouldSetCreatedAndUpdatedTimestamps() {
        // Arrange
        Event event = new Event();

        // Act: Manually invoke the @PrePersist lifecycle hook method
        event.onCreate();

        // Assert
        assertNotNull(event.getCreatedAt());
        assertNotNull(event.getUpdatedAt());
        // Verify both timestamps are aligned initialized values
        assertEquals(event.getCreatedAt(), event.getUpdatedAt());
    }

    @Test
    void onUpdate_ShouldModifyOnlyUpdatedTimestamp() throws InterruptedException {
        // Arrange
        Event event = new Event();
        event.onCreate(); // Initializes createdAt and updatedAt

        Instant initialCreatedAt = event.getCreatedAt();

        // Small pause to guarantee system clock progression
        Thread.sleep(5);

        // Act: Manually invoke the @PreUpdate lifecycle hook method
        event.onUpdate();

        // Assert
        assertEquals(initialCreatedAt, event.getCreatedAt(), "CreatedAt must remain unchanged during update operations");
        assertTrue(event.getUpdatedAt().isAfter(initialCreatedAt), "UpdatedAt must be freshly bumped forward");
    }

}
