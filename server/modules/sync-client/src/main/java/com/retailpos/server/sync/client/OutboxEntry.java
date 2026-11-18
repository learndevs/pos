package com.retailpos.server.sync.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A finished transaction waiting to be uploaded to the admin archive. */
@Entity
@Table(name = "txn_outbox")
public class OutboxEntry {

    @Id @Column(name = "transaction_id") private UUID transactionId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error") private String lastError;

    protected OutboxEntry() {}

    OutboxEntry(UUID transactionId, String payload, Instant now) {
        this.transactionId = transactionId;
        this.payload = payload;
        this.createdAt = now;
    }

    void sent(Instant now) { this.sentAt = now; this.lastError = null; this.attempts++; }
    void failed(String error) { this.attempts++; this.lastError = error; }

    public UUID getTransactionId() { return transactionId; }
    public String getPayload() { return payload; }
}
