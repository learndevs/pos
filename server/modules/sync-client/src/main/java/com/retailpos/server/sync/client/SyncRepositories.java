package com.retailpos.server.sync.client;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SyncStateRepository extends JpaRepository<SyncState, String> {}

interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {
    List<OutboxEntry> findTop100BySentAtIsNullOrderByCreatedAtAsc();
    long countBySentAtIsNull();
}
