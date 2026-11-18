package com.retailpos.server.cash;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CashService {

    private final CashSessionRepository sessions;
    private final Clock clock;

    public CashService(CashSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<CashSession> current() {
        return sessions.findOpen();
    }

    /** The open session, or 409 if the day has not been started. */
    @Transactional(readOnly = true)
    public CashSession requireOpen() {
        return current().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Start the day before selling."));
    }

    /** Day start: opens today's session with the counted float. */
    public CashSession open(UUID userId, long openingFloatMinor) {
        if (current().isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "The day is already open.");
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));
        if (sessions.existsByBusinessDate(today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Business day " + today + " was already closed.");
        }
        return sessions.save(CashSession.open(today, userId, openingFloatMinor, clock.instant()));
    }
}
