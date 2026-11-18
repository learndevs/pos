package com.retailpos.server.sync.client;

import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.Contract.BatchResult;
import com.retailpos.server.sync.contract.Contract.PackageAck;
import com.retailpos.server.sync.contract.Contract.PackageInfo;
import com.retailpos.server.sync.contract.Contract.TransactionBatch;
import com.retailpos.server.sync.contract.Contract.TransactionV1;
import com.retailpos.server.sync.contract.PackageZip;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Talks to the admin app (the till always calls out): every pos.sync.interval-ms it
 *   1. fetches packages waiting for this till, verifies + applies each (one DB transaction), acks APPLIED / FAILED;
 *   2. uploads finished transactions from the outbox to the archive.
 * Disabled when pos.sync.admin-url or terminal-token is empty (stand-alone till).
 */
@Service
public class SyncClient {

    public record Status(boolean enabled, String adminUrl, long lastPackage, Instant lastPackageAt, Instant lastSyncAt,
                         String lastError, Instant lastErrorAt, long outboxPending) {}

    private static final Logger log = LoggerFactory.getLogger(SyncClient.class);

    private final SyncApplier applier;
    private final SyncStateRepository state;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    private final Clock clock;
    private final String adminUrl;
    private final String token;
    private final RestClient http;
    private final ReentrantLock running = new ReentrantLock();

    SyncClient(SyncApplier applier, SyncStateRepository state, OutboxRepository outbox, ObjectMapper json, Clock clock,
               @Value("${pos.sync.admin-url:}") String adminUrl, @Value("${pos.sync.terminal-token:}") String token) {
        this.applier = applier;
        this.state = state;
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
        this.adminUrl = adminUrl.replaceAll("/+$", "");
        this.token = token;
        this.http = RestClient.builder().baseUrl(this.adminUrl.isEmpty() ? "http://localhost" : this.adminUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    public boolean enabled() {
        return !adminUrl.isEmpty() && !token.isEmpty();
    }

    @Scheduled(initialDelayString = "${pos.sync.initial-delay-ms:5000}", fixedDelayString = "${pos.sync.interval-ms:60000}")
    void scheduled() {
        if (enabled()) syncNow();
    }

    /** Fetch + apply packages, then upload the outbox. Safe to call any time; one run at a time. */
    public Status syncNow() {
        if (!enabled() || !running.tryLock()) return status();
        try {
            pullPackages();
            pushTransactions();
            touch(null);
        } catch (RuntimeException e) {
            log.warn("Sync failed: {}", e.getMessage());
            touch(message(e));
        } finally {
            running.unlock();
        }
        return status();
    }

    private void pullPackages() {
        List<PackageInfo> waiting = http.get().uri("/sync/v1/packages").retrieve().body(new ParameterizedTypeReference<>() {});
        for (PackageInfo p : waiting == null ? List.<PackageInfo>of() : waiting) {
            byte[] zip = http.get().uri("/sync/v1/packages/{no}/zip", p.packageNo()).accept(MediaType.parseMediaType("application/zip"))
                .retrieve().body(byte[].class);
            try {
                applyZip(zip);
                ack(p.packageNo(), PackageAck.applied());
                log.info("Sync: applied package #{} ({} items)", p.packageNo(), p.counts());
            } catch (RuntimeException e) {
                ack(p.packageNo(), PackageAck.failed(message(e)));
                throw new IllegalStateException("Package #" + p.packageNo() + " failed: " + message(e), e);
            }
        }
    }

    /** Verify + apply a package zip (also used for manual import from a USB stick). Returns the package number. */
    public int applyZip(byte[] zip) {
        var read = PackageZip.read(json, zip);
        applier.apply(read.manifest(), read.content());
        return read.manifest().packageNo();
    }

    private void ack(int packageNo, PackageAck ack) {
        http.post().uri("/sync/v1/packages/{no}/ack", packageNo).contentType(MediaType.APPLICATION_JSON).body(ack).retrieve().toBodilessEntity();
    }

    private void pushTransactions() {
        List<OutboxEntry> pending = outbox.findTop100BySentAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) return;
        List<TransactionV1> txns = pending.stream().map(o -> json.readValue(o.getPayload(), TransactionV1.class)).toList();
        try {
            BatchResult r = http.post().uri("/sync/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .body(new TransactionBatch(Contract.CONTRACT_VERSION, txns)).retrieve().body(BatchResult.class);
            Instant now = clock.instant();
            pending.forEach(o -> o.sent(now));
            outbox.saveAll(pending);
            log.info("Sync: uploaded {} transaction(s), {} new in archive", txns.size(), r == null ? "?" : r.stored());
        } catch (RuntimeException e) {
            pending.forEach(o -> o.failed(message(e)));
            outbox.saveAll(pending);
            throw e;
        }
    }

    private void touch(String error) {
        state.findById("admin").ifPresent(s -> {
            s.checked(clock.instant());
            if (error != null) s.failed(error, clock.instant());
            state.save(s);
        });
    }

    public Status status() {
        SyncState s = state.findById("admin").orElseThrow();
        return new Status(enabled(), adminUrl, s.getLastPackage(), s.getLastPackageAt(), s.getLastSyncAt(), s.getLastError(),
            s.getLastErrorAt(), outbox.countBySentAtIsNull());
    }

    private static String message(RuntimeException e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 500 ? m.substring(0, 500) : m;
    }
}
