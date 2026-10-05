package com.rush.ticket.payment.scheduler;

import com.rush.ticket.payment.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class OutboxProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    companion object {
        private val log = LoggerFactory.getLogger(OutboxProcessor::class.java)
        private const val TOPIC = "payment-results-topic"
        private const val BATCH_SIZE = 10
        private const val KAFKA_TIMEOUT_SEC = 4L
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun processPendingResultEvents() {
        val pageable = PageRequest.of(0, BATCH_SIZE)
        val pending = outboxEventRepository.findPendingLimit("PENDING", pageable)

        if (pending.isEmpty()) return;

        for (event in pending) {
            val eventId: UUID = event.id ?: continue

            try {
                kafkaTemplate.send(TOPIC, event.aggregateId, event.payload)
                    .get(KAFKA_TIMEOUT_SEC, TimeUnit.SECONDS)

                event.status = "SENT"
                event.processedAt = Instant.now()
            } catch (ex: Exception) {
                event.status = "FAILED"
                event.processedAt = Instant.now()
                log.error("Failed to publish outbox event [ID: {}] to Kafka", eventId, ex)
            }

            outboxEventRepository.save(event)
        }
    }

}