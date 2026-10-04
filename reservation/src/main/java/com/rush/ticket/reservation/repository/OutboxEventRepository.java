package com.rush.ticket.reservation.repository;

import com.rush.ticket.reservation.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(String status);
}
