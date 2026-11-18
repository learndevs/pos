package com.retailpos.server.sync.server;

import com.retailpos.server.common.PosException;
import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.Contract.BatchResult;
import com.retailpos.server.sync.contract.Contract.TransactionBatch;
import com.retailpos.server.sync.contract.Contract.TransactionV1;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Stores uploaded till transactions (idempotent by id) and serves them to the admin screens. */
@Service
@Transactional
public class ArchiveService {

    public record Summary(UUID id, UUID terminalId, long txnNo, String receiptNo, String status, LocalDate businessDate,
                          long totalMinor, Instant finishedAt, Instant receivedAt) {}

    public record Detail(Summary summary, TransactionV1 transaction) {}

    private final ArchiveRepository archive;
    private final ObjectMapper json;

    ArchiveService(ArchiveRepository archive, ObjectMapper json) {
        this.archive = archive;
        this.json = json;
    }

    public BatchResult store(Terminal till, TransactionBatch batch) {
        if (batch.contractVersion() != Contract.CONTRACT_VERSION) {
            throw PosException.badRequest("Unsupported sync contract v" + batch.contractVersion() + ".");
        }
        int stored = 0;
        for (TransactionV1 t : batch.transactions()) {
            Instant finished = t.completedAt() != null ? t.completedAt() : t.cancelledAt();
            stored += archive.insertIfAbsent(t.id(), till.getId(), t.txnNo(), t.receiptNo(), t.status(), t.businessDate(),
                t.totalMinor(), finished, json.writeValueAsString(t));
        }
        return new BatchResult(batch.transactions().size(), stored);
    }

    @Transactional(readOnly = true)
    public List<Summary> search(UUID terminalId, LocalDate from, LocalDate to, String q) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return archive.search(terminalId, from, to, term, PageRequest.of(0, 200)).stream().map(ArchiveService::summary).toList();
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        ArchivedTransaction a = archive.findById(id).orElseThrow(() -> PosException.notFound("Transaction not found."));
        return new Detail(summary(a), json.readValue(a.getPayload(), TransactionV1.class));
    }

    private static Summary summary(ArchivedTransaction a) {
        return new Summary(a.getId(), a.getTerminalId(), a.getTxnNo(), a.getReceiptNo(), a.getStatus(), a.getBusinessDate(),
            a.getTotalMinor(), a.getFinishedAt(), a.getReceivedAt());
    }
}
