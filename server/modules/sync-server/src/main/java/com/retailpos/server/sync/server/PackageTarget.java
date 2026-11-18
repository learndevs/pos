package com.retailpos.server.sync.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Delivery status of one package to one till: PENDING -> DOWNLOADED -> APPLIED | FAILED (retried). */
@Entity
@Table(name = "package_target")
@IdClass(PackageTarget.Key.class)
public class PackageTarget {

    public record Key(Integer packageNo, UUID terminalId) implements Serializable {
        public Key() { this(null, null); }
    }

    @Id @Column(name = "package_no") private Integer packageNo;
    @Id @Column(name = "terminal_id") private UUID terminalId;
    @Column(nullable = false) private String status;
    private String error;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PackageTarget() {}

    PackageTarget(int packageNo, UUID terminalId, Instant now) {
        this.packageNo = packageNo;
        this.terminalId = terminalId;
        this.status = "PENDING";
        this.updatedAt = now;
    }

    void mark(String status, String error, Instant now) {
        this.status = status;
        this.error = error;
        this.updatedAt = now;
    }

    public Integer getPackageNo() { return packageNo; }
    public UUID getTerminalId() { return terminalId; }
    public String getStatus() { return status; }
    public String getError() { return error; }
    public Instant getUpdatedAt() { return updatedAt; }
}
