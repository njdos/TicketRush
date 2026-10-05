package com.rush.ticket.payment.dto.reqResp

import java.util.*

data class PaymentResultPayload(
        val eventId: UUID,
        val reservationId: UUID,
        val success: Boolean
)
