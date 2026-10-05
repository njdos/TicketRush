package com.rush.ticket.payment.config

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
class PaymentMockConfig {

    private val _failureRatePercent = AtomicInteger(20)
    var failureRatePercent: Int
        get() = _failureRatePercent.get()
        set(value) {
            require(value in 0..100) { "failureRatePercent must be between 0 and 100, but was $value" }
            _failureRatePercent.set(value)
        }

}