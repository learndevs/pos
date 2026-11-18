package com.retailpos.server.audit;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only audit record (the DB rejects UPDATE/DELETE). */
@Entity
@Table(name = "audit_log")
public class AuditEntry extends BaseEntity {

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(updatable = false)
    private String entity;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEntry() {}

    AuditEntry(UUID userId, String action, String entity, UUID entityId, String details, Instant now) {
        super(true);
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = now;
    }

    public UUID getUserId() { return userId; }
    public String getAction() { return action; }
}
