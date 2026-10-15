package com.rush.ticket.reservation.service;

import com.rush.ticket.reservation.entity.Seat;
import com.rush.ticket.reservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {


    private final SeatRepository seatRepository;

    @Override
    public Set<Seat> seatsByEventId(UUID eventId) {
        return seatRepository.findAllByEventId(eventId);
    }

}