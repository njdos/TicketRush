package com.rush.ticket.reservation.controller;

import com.rush.ticket.reservation.dto.api.ApiResponse;
import com.rush.ticket.reservation.dto.req.ReservationRequestDto;
import com.rush.ticket.reservation.dto.resp.ReservationResponseDto;
import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDto>> reserve(
            @Valid @RequestBody ReservationRequestDto request,
            @RequestHeader("X-User-Id") @NotNull UUID userId
    ) {
        Reservation reservation = reservationService.reserveSeat(request.eventId(), request.seatId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ReservationResponseDto.from(reservation)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable @NotNull UUID id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<String>> pay(@PathVariable @NotNull UUID id) {
        reservationService.requestPayment(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Payment requested, processing via Kafka"));
    }
}