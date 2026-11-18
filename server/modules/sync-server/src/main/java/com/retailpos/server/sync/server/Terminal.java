package com.retailpos.server.sync.server;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** A till allowed to pull the change feed. Only a SHA-256 of its token is stored. */
@Entity
@Table(name = "terminal")
public class Terminal extends BaseEntity {

    @Column(nullable = false) private String name;
    @Column(name = "token_sha256", nullable = false, unique = true) private String tokenSha256;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "last_seen_at") private Instant lastSeenAt;

    protected Terminal() {}

    Terminal(String name, String tokenSha256, Instant now) {
        super(true);
        this.name = name;
        this.tokenSha256 = tokenSha256;
        this.active = true;
        this.createdAt = now;
    }

    void seen(Instant now) {
        this.lastSeenAt = now;
    }

    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
