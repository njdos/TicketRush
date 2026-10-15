package com.rush.ticket.reservation.controller;

import com.rush.ticket.reservation.dto.api.ApiResponse;
import com.rush.ticket.reservation.entity.Seat;
import com.rush.ticket.reservation.service.SeatService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservation/seats")
@Validated
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }


    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Set<Seat>>> getSeats(@PathVariable @NotNull UUID eventId) {
        Set<Seat> seats = seatService.seatsByEventId(eventId);
        return ResponseEntity.ok(ApiResponse.success(seats));
    }
}