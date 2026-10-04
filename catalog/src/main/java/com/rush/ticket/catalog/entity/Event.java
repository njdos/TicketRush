package com.rush.ticket.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_venue_starts", columnList = "venue, starts_at"),
                @Index(name = "idx_events_name", columnList = "name")
        }
)
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Event() {
    }

    public Event(String name, String venue, LocalDateTime startsAt,
                 Integer totalSeats, BigDecimal price, UUID organizerId) {
        this.name = name;
        this.venue = venue;
        this.startsAt = startsAt;
        this.totalSeats = totalSeats;
        this.price = price;
        this.organizerId = organizerId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String name, String venue, LocalDateTime startsAt, BigDecimal price) {
        this.name = name;
        this.venue = venue;
        this.startsAt = startsAt;
        this.price = price;
    }

}