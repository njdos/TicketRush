package com.rush.ticket.reservation.repository;

import com.rush.ticket.reservation.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {}