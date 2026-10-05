package com.rush.ticket.payment.dto.reqResp

import java.math.BigDecimal
import java.util.*

data class PaymentRequestedEvent(
        val reservationId: UUID,
        val userId: UUID,
        val totalAmount: BigDecimal
)