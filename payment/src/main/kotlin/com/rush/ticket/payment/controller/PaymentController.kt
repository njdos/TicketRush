package com.rush.ticket.payment.controller;

import com.rush.ticket.catalog.dto.base.ApiResponse
import com.rush.ticket.payment.config.PaymentMockConfig
import com.rush.ticket.payment.entity.Payment
import com.rush.ticket.payment.service.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.properties.Delegates

@RestController
@RequestMapping("/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val mockConfig: PaymentMockConfig
) {

    @GetMapping("/{reservationId}")
    fun getPayment(@PathVariable reservationId: UUID): ResponseEntity<ApiResponse<Payment?>> {
        val payment = paymentService.getPaymentByReservationId(reservationId)
        return ResponseEntity.ok(ApiResponse.success(payment))
    }

    @PutMapping("/admin/payment-config")
    fun setFailureRate(@RequestParam failureRatePercent: Int): ResponseEntity<ApiResponse<String>> {
        mockConfig.failureRatePercent = failureRatePercent
        Delegates.notNull<Int>()
        val message = "Payment failure rate set to $failureRatePercent%"
        return ResponseEntity.ok(ApiResponse.success(message))
    }
}