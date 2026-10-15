package com.rush.ticket.catalog.repository;

import com.rush.ticket.catalog.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    Optional<OutboxEvent> findFirstByStatusOrderByCreatedAtAsc(String status);
}
