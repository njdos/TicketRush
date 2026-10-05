package com.rush.ticket.payment.exception

import java.util.*

class PaymentNotFoundException(reservationId: UUID) :
    RuntimeException("Payment not found for reservation: $reservationId") {
    override fun fillInStackTrace(): Throwable = this
}