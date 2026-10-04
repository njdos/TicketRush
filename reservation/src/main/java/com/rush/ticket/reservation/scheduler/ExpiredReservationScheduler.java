package com.rush.ticket.reservation.scheduler;

import com.rush.ticket.reservation.entity.Reservation;
import com.rush.ticket.reservation.entity.ReservationStatus;
import com.rush.ticket.reservation.repository.ReservationRepository;
import com.rush.ticket.reservation.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ExpiredReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredReservationScheduler.class);

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    public ExpiredReservationScheduler(ReservationRepository reservationRepository, SeatRepository seatRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void releaseExpiredHolds() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.HELD, Instant.now());

        for (Reservation reservation : expired) {
            reservation.expire();
            seatRepository.findById(reservation.getSeatId()).ifPresent(seat -> {
                seat.markFree();
                seatRepository.save(seat);
            });
            log.info("Reservation {} expired, seat {} released", reservation.getId(), reservation.getSeatId());
        }
    }
}