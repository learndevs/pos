package com.retailpos.server.cash;

import com.retailpos.server.identity.SessionUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Day start (open cash session). Day end comes later. */
@RestController
@RequestMapping("/api/cash/session")
public class CashSessionController {

    public record SessionDto(UUID id, LocalDate businessDate, long openingFloat) {
        static SessionDto of(CashSession s) {
            return new SessionDto(s.getId(), s.getBusinessDate(), s.getOpeningFloat());
        }
    }

    public record OpenRequest(@NotNull @Min(0) @Max(100_000_000) Long openingFloatMinor) {}

    private final CashService cash;

    public CashSessionController(CashService cash) {
        this.cash = cash;
    }

    @GetMapping
    public ResponseEntity<SessionDto> current() {
        return cash.current().map(s -> ResponseEntity.ok(SessionDto.of(s))).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/open")
    @PreAuthorize("hasAuthority('DAY_OPEN')")
    public SessionDto open(@Valid @RequestBody OpenRequest req, @AuthenticationPrincipal SessionUser user) {
        return SessionDto.of(cash.open(user.id(), req.openingFloatMinor()));
    }
}
