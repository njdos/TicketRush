package com.rush.ticket.reservation.service;

import com.rush.ticket.reservation.entity.Seat;

import java.util.Set;
import java.util.UUID;

public interface SeatService {
    Set<Seat> seatsByEventId(UUID eventId);
}