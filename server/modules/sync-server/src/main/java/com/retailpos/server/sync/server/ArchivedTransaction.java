package com.retailpos.server.sync.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A finished till transaction uploaded for the archive (full JSON in payload). Read-only here. */
@Entity
@Table(name = "transaction_archive")
public class ArchivedTransaction {

    @Id private UUID id;
    @Column(name = "terminal_id", nullable = false) private UUID terminalId;
    @Column(name = "txn_no", nullable = false) private long txnNo;
    @Column(name = "receipt_no") private String receiptNo;
    @Column(nullable = false) private String status;
    @Column(name = "business_date", nullable = false) private LocalDate businessDate;
    @Column(name = "total_minor", nullable = false) private long totalMinor;
    @Column(name = "device_code") private String deviceCode;
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;

    protected ArchivedTransaction() {}

    public UUID getId() { return id; }
    public UUID getTerminalId() { return terminalId; }
    public long getTxnNo() { return txnNo; }
    public String getReceiptNo() { return receiptNo; }
    public String getStatus() { return status; }
    public LocalDate getBusinessDate() { return businessDate; }
    public long getTotalMinor() { return totalMinor; }
    public String getDeviceCode() { return deviceCode; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getPayload() { return payload; }
}
