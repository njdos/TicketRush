package com.rush.ticket.payment.service

import com.rush.ticket.payment.dto.reqResp.PaymentRequestedEvent
import com.rush.ticket.payment.entity.Payment
import java.util.*

interface PaymentService {
    fun processPaymentRequest(event: PaymentRequestedEvent)
    fun getPaymentByReservationId(reservationId: UUID): Payment?
}