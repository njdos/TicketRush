package com.rush.ticket.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.rush.ticket.payment.config.PaymentMockConfig
import com.rush.ticket.payment.dto.reqResp.PaymentRequestedEvent
import com.rush.ticket.payment.dto.reqResp.PaymentResultPayload
import com.rush.ticket.payment.entity.OutboxEvent
import com.rush.ticket.payment.entity.Payment
import com.rush.ticket.payment.entity.PaymentStatus
import com.rush.ticket.payment.exception.PaymentNotFoundException
import com.rush.ticket.payment.repository.OutboxEventRepository
import com.rush.ticket.payment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val mockConfig: PaymentMockConfig,
    private val objectMapper: ObjectMapper
) : PaymentService {

    companion object {
        private val log = LoggerFactory.getLogger(PaymentServiceImpl::class.java)
    }

    @Transactional
    override fun processPaymentRequest(event: PaymentRequestedEvent) {
        paymentRepository.findByReservationId(event.reservationId)?.let {
            log.warn("Payment for reservation {} already processed, skipping", event.reservationId)
            return
        }

        val failed: Boolean = ThreadLocalRandom.current().nextInt(100) < mockConfig.failureRatePercent
        val status: PaymentStatus = if (failed) PaymentStatus.FAILED else PaymentStatus.SUCCESS

        val payment = try {
            paymentRepository.save(
                Payment(event.reservationId, event.userId, event.totalAmount, status)
            )
        } catch (e: DataIntegrityViolationException) {
            log.warn("Duplicate payment insert caught by unique constraint for reservation {}", event.reservationId)
            return
        }

        log.info("Payment {} for reservation {}: {}", payment.id, event.reservationId, status)

        val resultPayload = PaymentResultPayload(
            UUID.randomUUID(),
            event.reservationId,
            status == PaymentStatus.SUCCESS
        )

        try {
            val json = objectMapper.writeValueAsString(resultPayload)
            outboxEventRepository.save(
                OutboxEvent(event.reservationId.toString(), "PaymentResult", json)
            )
        } catch (e: JsonProcessingException) {
            log.error("Failed to serialize PaymentResultPayload for reservation {}", event.reservationId, e)
            throw RuntimeException("Outbox serialization failed", e)
        }
    }

    override fun getPaymentByReservationId(reservationId: UUID): Payment =
        paymentRepository.findByReservationId(reservationId)
            ?: throw PaymentNotFoundException(reservationId);

}