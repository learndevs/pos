package com.retailpos.server.common;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Base for entities with an app-assigned UUIDv7 id. Implements Persistable so Spring Data
 * INSERTs new objects directly instead of SELECT-then-merge (ids are never null).
 */
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew;

    protected BaseEntity() {}

    /** New row with a known id (e.g. created by the admin app and synced to the till). */
    protected BaseEntity(UUID id) {
        this.id = id;
        this.isNew = true;
    }

    /** For new rows created in code. */
    protected BaseEntity(boolean newRow) {
        this.id = UuidV7.next();
        this.isNew = newRow;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
