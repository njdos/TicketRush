package com.rush.ticket.reservation.exception;

public class SeatAlreadyReservedException extends RuntimeException {
    public SeatAlreadyReservedException(String seatId) {
        super("Seat already reserved: " + seatId);
    }
}