package com.retailpos.server.admin;

import com.retailpos.server.sync.contract.Contract.Entity;
import com.retailpos.server.sync.server.ArchiveService;
import com.retailpos.server.sync.server.PackageService;
import com.retailpos.server.sync.server.Terminals;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin screens for packages (create & broadcast, status per till, zip download), terminals and the archive. */
@RestController
@RequestMapping("/admin/api")
class AdminSync {

    record PackageRequest(@NotEmpty Set<Entity> entities, @NotNull PackageService.Scope scope, LocalDate dateFrom,
                          LocalDate dateTo, @NotEmpty List<UUID> terminalIds, @Size(max = 200) String note) {}
    record TerminalRequest(@NotBlank @Size(max = 60) String name) {}
    record TerminalView(UUID id, String name, boolean active, Instant lastSeenAt, Integer lastAppliedPackage) {}

    private final PackageService packages;
    private final Terminals terminals;
    private final ArchiveService archive;

    AdminSync(PackageService packages, Terminals terminals, ArchiveService archive) {
        this.packages = packages;
        this.terminals = terminals;
        this.archive = archive;
    }

    // packages
    @GetMapping("/packages")
    List<PackageService.PackageView> packages() {
        return packages.recent();
    }

    @PostMapping("/packages")
    PackageService.PackageView create(@Valid @RequestBody PackageRequest req, @AuthenticationPrincipal AdminAuth.Principal me) {
        return packages.build(new PackageService.BuildRequest(req.entities(), req.scope(), req.dateFrom(), req.dateTo(),
            req.terminalIds(), req.note()), me.id());
    }

    /** For offline tills: import this zip on the till (USB). */
    @GetMapping(value = "/packages/{no}/zip", produces = "application/zip")
    ResponseEntity<byte[]> zip(@PathVariable int no) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"package-" + no + ".zip\"")
            .body(packages.get(no).getZip());
    }

    // terminals
    @GetMapping("/terminals")
    List<TerminalView> terminals() {
        return terminals.list().stream().map(t -> {
            var last = packages.lastApplied(t.getId());
            return new TerminalView(t.getId(), t.getName(), t.isActive(), t.getLastSeenAt(),
                last.isEmpty() ? null : ((Number) last.getFirst()[0]).intValue());
        }).toList();
    }

    /** The token is returned once; configure it on the till (pos.sync.terminal-token). */
    @PostMapping("/terminals")
    Map<String, Object> createTerminal(@Valid @RequestBody TerminalRequest req) {
        var created = terminals.create(req.name());
        return Map.of("id", created.terminal().getId(), "name", created.terminal().getName(), "token", created.token());
    }

    // transaction archive
    @GetMapping("/transactions")
    List<ArchiveService.Summary> transactions(@RequestParam(required = false) UUID terminalId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(required = false) String q) {
        return archive.search(terminalId, from, to, q);
    }

    @GetMapping("/transactions/{id}")
    ArchiveService.Detail transaction(@PathVariable UUID id) {
        return archive.detail(id);
    }
}
