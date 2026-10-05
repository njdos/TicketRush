package com.rush.ticket.payment.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rush.ticket.payment.dto.reqResp.PaymentRequestedEvent
import com.rush.ticket.payment.service.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class PaymentRequestListener(
    private val paymentService: PaymentService,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val log = LoggerFactory.getLogger(PaymentRequestListener::class.java)
        private const val TOPIC = "seats-generated-topic"
        private val IDEMPOTENCY_TTL = Duration.ofDays(1)
    }

    @KafkaListener(topics = [TOPIC], groupId = "payment-group")
    fun handlePaymentRequested(@Payload messagePayload: String, ack: Acknowledgment) {
        val event = try {
            objectMapper.readValue<PaymentRequestedEvent>(messagePayload)
        } catch (ex: Exception) {
            log.error("Poison pill detected! Failed to deserialize payload: {}", messagePayload, ex)
            ack.acknowledge()
            return
        }

        val idempotencyKey = "payment_idempotency:${event.reservationId}"
        val isNewEntry = redisTemplate.opsForValue()
            .setIfAbsent(idempotencyKey, "PROCESSING", IDEMPOTENCY_TTL)

        if (isNewEntry != true) {
            val currentStatus = redisTemplate.opsForValue().get(idempotencyKey)
            if (currentStatus == "PROCESSED") {
                log.warn("Duplicate payment event ignored. Reservation already processed: {}", event.reservationId)
                ack.acknowledge()
            } else {
                log.warn("Payment event is already being processed by another pod: {}", event.reservationId)
            }
            return
        }

        try {
            paymentService.processPaymentRequest(event)
            redisTemplate.opsForValue().set(idempotencyKey, "PROCESSED", IDEMPOTENCY_TTL)
            ack.acknowledge()
        } catch (ex: Exception) {
            log.error("Business logic execution failed for reservation: {}. Evicting idempotency key.", event.reservationId, ex)
            redisTemplate.delete(idempotencyKey)
            throw ex
        }
    }
}
