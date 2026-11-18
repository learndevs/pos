package com.retailpos.server.sync.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** This till's sync status (one row 'admin'). */
@Entity
@Table(name = "sync_state")
public class SyncState {

    @Id private String id;
    @Column(name = "last_package", nullable = false) private long lastPackage;
    @Column(name = "last_package_at") private Instant lastPackageAt;
    @Column(name = "last_sync_at") private Instant lastSyncAt;
    @Column(name = "last_error") private String lastError;
    @Column(name = "last_error_at") private Instant lastErrorAt;

    protected SyncState() {}

    void packageApplied(int packageNo, Instant now) {
        this.lastPackage = Math.max(this.lastPackage, packageNo);
        this.lastPackageAt = now;
        this.lastError = null;
        this.lastErrorAt = null;
    }

    void checked(Instant now) {
        this.lastSyncAt = now;
    }

    void failed(String error, Instant now) {
        this.lastError = error;
        this.lastErrorAt = now;
    }

    public long getLastPackage() { return lastPackage; }
    public Instant getLastPackageAt() { return lastPackageAt; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public String getLastError() { return lastError; }
    public Instant getLastErrorAt() { return lastErrorAt; }
}
