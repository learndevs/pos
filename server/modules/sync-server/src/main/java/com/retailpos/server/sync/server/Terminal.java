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
    /** Short device code (T01) printed on receipts; unique. */
    @Column(nullable = false, unique = true) private String code;
    /** Till number shown to staff (1, 2, 3...); unique. */
    @Column(name = "till_no", nullable = false, unique = true) private int tillNo;
    private String location;
    @Column(name = "token_sha256", nullable = false, unique = true) private String tokenSha256;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "last_seen_at") private Instant lastSeenAt;

    protected Terminal() {}

    Terminal(int tillNo, String name, String code, String location, String tokenSha256, Instant now) {
        super(true);
        this.tillNo = tillNo;
        this.name = name;
        this.code = code;
        this.location = location;
        this.tokenSha256 = tokenSha256;
        this.active = true;
        this.createdAt = now;
    }

    void update(int tillNo, String name, String code, String location) {
        this.tillNo = tillNo;
        this.name = name;
        this.code = code;
        this.location = location;
    }

    void newToken(String tokenSha256) {
        this.tokenSha256 = tokenSha256;
        this.active = true;
    }

    void disable() {
        this.active = false;
    }

    void seen(Instant now) {
        this.lastSeenAt = now;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public int getTillNo() { return tillNo; }
    public Instant getCreatedAt() { return createdAt; }
    public String getLocation() { return location; }
    public boolean isActive() { return active; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
