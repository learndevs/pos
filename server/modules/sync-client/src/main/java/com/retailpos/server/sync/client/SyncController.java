package com.retailpos.server.sync.client;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sync status (header chip), manual sync, and offline import of a package zip. */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    public record ImportResult(int packageNo, SyncClient.Status status) {}

    private final SyncClient sync;

    SyncController(SyncClient sync) {
        this.sync = sync;
    }

    @GetMapping("/status")
    public SyncClient.Status status() {
        return sync.status();
    }

    @PostMapping("/now")
    public SyncClient.Status now() {
        return sync.syncNow();
    }

    /** Offline shops: upload the package zip downloaded from admin (e.g. from a USB stick). */
    @PostMapping(value = "/import", consumes = {"application/zip", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ImportResult importZip(@RequestBody byte[] zip) {
        int no = sync.applyZip(zip);
        return new ImportResult(no, sync.status());
    }
}
