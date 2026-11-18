package com.retailpos.server.cash;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One business day of trading: opened with a float, closed with a count (day end comes later). */
@Entity
@Table(name = "cash_session")
public class CashSession extends BaseEntity {

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private String status;          // OPEN | CLOSED

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opening_float", nullable = false)
    private long openingFloat;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "expected_cash")
    private Long expectedCash;

    @Column(name = "counted_cash")
    private Long countedCash;

    private Long variance;

    private String notes;

    protected CashSession() {}

    private CashSession(boolean newRow) {
        super(newRow);
    }

    public static CashSession open(LocalDate businessDate, UUID userId, long openingFloat, Instant now) {
        CashSession s = new CashSession(true);
        s.businessDate = businessDate;
        s.status = "OPEN";
        s.openedBy = userId;
        s.openedAt = now;
        s.openingFloat = openingFloat;
        return s;
    }

    public boolean isOpen() { return "OPEN".equals(status); }
    public LocalDate getBusinessDate() { return businessDate; }
    public String getStatus() { return status; }
    public UUID getOpenedBy() { return openedBy; }
    public long getOpeningFloat() { return openingFloat; }
}
