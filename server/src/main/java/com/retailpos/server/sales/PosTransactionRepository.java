package com.retailpos.server.sales;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PosTransactionRepository extends JpaRepository<PosTransaction, UUID> {

    Optional<PosTransaction> findFirstByStatusAndCreatedByAndCashSessionIdOrderByCreatedAtDesc(
        String status, UUID createdBy, UUID cashSessionId);

    /** Next gap-free receipt number for the business date (atomic upsert on receipt_counter). */
    @Query(value = """
        INSERT INTO receipt_counter (business_date, last_no) VALUES (:businessDate, 1)
        ON CONFLICT (business_date) DO UPDATE SET last_no = receipt_counter.last_no + 1
        RETURNING last_no""", nativeQuery = true)
    int nextReceiptNo(LocalDate businessDate);
}
