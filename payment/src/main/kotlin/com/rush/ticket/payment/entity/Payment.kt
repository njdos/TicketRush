package com.rush.ticket.payment.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "payments")
class Payment(
    @Column(name = "reservation_id", nullable = false, unique = true)
    val reservationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: PaymentStatus
) {
    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        private set

    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }
}
