package com.retailpos.server.cash;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {

    Optional<CashSession> findFirstByStatus(String status);

    boolean existsByBusinessDate(LocalDate businessDate);

    default Optional<CashSession> findOpen() {
        return findFirstByStatus("OPEN");
    }
}
