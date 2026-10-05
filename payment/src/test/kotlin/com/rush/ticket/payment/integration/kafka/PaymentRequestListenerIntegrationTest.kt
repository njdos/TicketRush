//package com.rush.ticket.payment.integration.kafka
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.rush.ticket.catalog.integration.BaseIntegrationTest
//import com.rush.ticket.payment.dto.reqResp.PaymentRequestedEvent
//import com.rush.ticket.payment.repository.OutboxEventRepository
//import com.rush.ticket.payment.repository.PaymentRepository
//import org.assertj.core.api.Assertions.assertThat
//import org.awaitility.Awaitility
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.kafka.core.KafkaTemplate
//import java.math.BigDecimal
//import java.time.Duration
//import java.util.*
//
//class PaymentRequestListenerIntegrationTest : BaseIntegrationTest() {
//
//    @Autowired
//    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
//
//    @Autowired
//    private lateinit var paymentRepository: PaymentRepository
//
//    @Autowired
//    private lateinit var outboxEventRepository: OutboxEventRepository
//
//    @Autowired
//    private lateinit var objectMapper: ObjectMapper
//
//    @Test
//    fun `should process payment event and save to outbox table`() {
//        // Arrange
//        val reservationId = UUID.randomUUID()
//        val userId = UUID.randomUUID()
//        val event = PaymentRequestedEvent(
//            reservationId = reservationId,
//            userId = userId,
//            totalAmount = BigDecimal("150.00")
//        )
//        val payload = objectMapper.writeValueAsString(event)
//
//        kafkaTemplate.send("payment-requests-topic", reservationId.toString(), payload).get()
//
//        Awaitility.await()
//            .atMost(Duration.ofSeconds(15))
//            .pollInterval(Duration.ofMillis(200))
//            .untilAsserted {
//                val payments = paymentRepository.findAll()
//                val targetPayment = payments.find { it.reservationId == reservationId }
//                assertThat(targetPayment).isNotNull
//                assertThat(targetPayment!!.amount).isEqualByComparingTo("150.00")
//
//                val outboxEvents = outboxEventRepository.findAll()
//                val targetOutbox = outboxEvents.find { it.aggregateId == reservationId.toString() }
//                assertThat(targetOutbox).isNotNull
//                assertThat(targetOutbox!!.status).isEqualTo("SENT")
//            }
//    }
//}
