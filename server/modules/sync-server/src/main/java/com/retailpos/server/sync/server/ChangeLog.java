package com.retailpos.server.sync.server;

import com.retailpos.server.sync.contract.Contract;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Records every master-data edit (full record) so packages can include "what changed between two dates". */
@Service
@Transactional
public class ChangeLog {

    private final ChangeRecordRepository changes;
    private final ObjectMapper json;
    private final Clock clock;

    ChangeLog(ChangeRecordRepository changes, ObjectMapper json, Clock clock) {
        this.changes = changes;
        this.json = json;
        this.clock = clock;
    }

    /** Call in the same transaction as the edit, with the full record after the change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Contract.Entity entity, UUID id, Object recordV1, UUID adminUserId) {
        changes.save(new ChangeRecord(entity, id, json.writeValueAsString(recordV1), adminUserId, clock.instant()));
    }

    @Transactional(readOnly = true)
    List<UUID> changedIds(Contract.Entity entity, Instant from, Instant toExclusive) {
        return changes.changedIds(entity, from, toExclusive);
    }
}
