package com.rush.ticket.payment.repository

import com.rush.ticket.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface PaymentRepository : JpaRepository<Payment, UUID> {
    @Transactional(readOnly = true)
    fun findByReservationId(reservationId: UUID) : Payment?
}