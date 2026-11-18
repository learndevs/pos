package com.retailpos.server.sync.server;

import com.retailpos.server.common.PosException;
import com.retailpos.server.sync.contract.Contract.BatchResult;
import com.retailpos.server.sync.contract.Contract.PackageAck;
import com.retailpos.server.sync.contract.Contract.PackageInfo;
import com.retailpos.server.sync.contract.Contract.TransactionBatch;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for tills (they always call in). Auth: "Authorization: Bearer <terminal token>".
 *   GET  /sync/v1/packages               packages waiting for this till (not yet applied), oldest first
 *   GET  /sync/v1/packages/{no}/zip      the package zip
 *   POST /sync/v1/packages/{no}/ack      APPLIED | FAILED (+ error)
 *   POST /sync/v1/transactions           finished transactions for the archive (idempotent)
 */
@RestController
@RequestMapping("/sync/v1")
public class SyncTerminalController {

    private final Terminals terminals;
    private final PackageService packages;
    private final ArchiveService archive;

    SyncTerminalController(Terminals terminals, PackageService packages, ArchiveService archive) {
        this.terminals = terminals;
        this.packages = packages;
        this.archive = archive;
    }

    @GetMapping("/packages")
    public List<PackageInfo> waiting(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return packages.waitingFor(till(auth));
    }

    @GetMapping(value = "/packages/{no}/zip", produces = "application/zip")
    public ResponseEntity<byte[]> zip(@PathVariable int no, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        byte[] zip = packages.download(till(auth), no);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"package-" + no + ".zip\"").body(zip);
    }

    @PostMapping("/packages/{no}/ack")
    public void ack(@PathVariable int no, @RequestBody PackageAck ack,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        packages.acknowledge(till(auth), no, ack);
    }

    @PostMapping("/transactions")
    public BatchResult transactions(@RequestBody TransactionBatch batch,
                                    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return archive.store(till(auth), batch);
    }

    private Terminal till(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        return terminals.authenticate(token).orElseThrow(() -> PosException.forbidden("Unknown or disabled terminal."));
    }
}
