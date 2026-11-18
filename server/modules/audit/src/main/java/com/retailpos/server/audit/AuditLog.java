package com.retailpos.server.audit;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Writes audit_log entries. Joins the caller's transaction. */
@Component
public class AuditLog {

    private final AuditEntryRepository entries;
    private final Clock clock;

    public AuditLog(AuditEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    public void record(UUID userId, String action, String entity, UUID entityId, String detailsJson) {
        entries.save(new AuditEntry(userId, action, entity, entityId, detailsJson, clock.instant()));
    }
}
