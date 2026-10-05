package com.rush.ticket.payment.repository

import com.rush.ticket.payment.entity.OutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {

    @Transactional(readOnly = true)
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    fun findPendingLimit(status: String, pageable: Pageable): List<OutboxEvent>

}