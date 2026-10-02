package com.rush.ticket.reservation.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @Column(name = "seat_id")
    private UUID seatId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.FREE;

    @Version
    private Long version; // optimistic-lock запасний захист, якщо Redis недоступний

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Seat() {}

    public Seat(UUID seatId, UUID eventId) {
        this.seatId = seatId;
        this.eventId = eventId;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public void markHeld() {
        if (status != SeatStatus.FREE) {
            throw new IllegalStateException("Seat " + seatId + " is not FREE, current status: " + status);
        }
        this.status = SeatStatus.HELD;
    }

    public void markSold() { this.status = SeatStatus.SOLD; }
    public void markFree() { this.status = SeatStatus.FREE; }

    public UUID getSeatId() { return seatId; }
    public UUID getEventId() { return eventId; }
    public SeatStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}