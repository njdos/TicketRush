package com.rush.ticket.reservation.controller;

import com.rush.ticket.reservation.dto.ApiResponse;
import com.rush.ticket.reservation.dto.ReservationRequestDto;
import com.rush.ticket.reservation.dto.ReservationResponseDto;
import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDto>> reserve(
            @Valid @RequestBody ReservationRequestDto request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        Reservation reservation = reservationService.reserveSeat(request.eventId(), request.seatId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ReservationResponseDto.from(reservation)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam UUID eventId,
            @RequestParam UUID seatId
    ) {
        reservationService.cancelReservation(id, eventId, seatId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<String>> pay(@PathVariable UUID id) {
        // TODO: після підключення Kafka — публікація PaymentRequested замість заглушки
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Payment requested (stub, Kafka not wired yet)"));
    }
}