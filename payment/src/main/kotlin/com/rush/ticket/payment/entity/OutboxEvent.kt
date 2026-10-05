package com.rush.ticket.payment.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: String,

    @Column(name = "event_type", nullable = false)
    var eventType: String,

    @Column(nullable = false)
    var payload: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null;

    @Column(nullable = false)
    var status: String = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        private set

    @Column(name = "processed_at")
    var processedAt: Instant? = null

    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }

}
