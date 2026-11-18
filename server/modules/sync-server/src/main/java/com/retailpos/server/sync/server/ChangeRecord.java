package com.retailpos.server.sync.server;

import com.retailpos.server.sync.contract.Contract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One change to master data (full record as JSON). Used to find what changed in a date range. */
@Entity
@Table(name = "config_change")
public class ChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Contract.Entity entity;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChangeRecord() {}

    ChangeRecord(Contract.Entity entity, UUID entityId, String payload, UUID createdBy, Instant now) {
        this.entity = entity;
        this.entityId = entityId;
        this.payload = payload;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public Long getSeq() { return seq; }
    public Contract.Entity getEntity() { return entity; }
    public UUID getEntityId() { return entityId; }
    public String getPayload() { return payload; }
}
