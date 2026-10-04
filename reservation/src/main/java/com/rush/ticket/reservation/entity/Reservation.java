package com.rush.ticket.reservation.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.HELD;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {}

    public Reservation(UUID userId, UUID seatId, UUID eventId, Instant expiresAt) {
        this.userId = userId;
        this.seatId = seatId;
        this.eventId = eventId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); this.updatedAt = this.createdAt; }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void confirm() { this.status = ReservationStatus.CONFIRMED; }
    public void release() { this.status = ReservationStatus.RELEASED; }
    public void expire() { this.status = ReservationStatus.EXPIRED; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getSeatId() { return seatId; }
    public UUID getEventId() { return eventId; }
    public ReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}