package com.retailpos.server.identity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory sessions. Single terminal, loopback only: a server restart simply signs everyone out.
 * Sessions expire after an idle timeout; each use extends them.
 */
@Component
public class SessionStore {

    static final Duration IDLE_TIMEOUT = Duration.ofHours(12);

    private record Entry(SessionUser user, Instant lastSeen) {}

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
    private final Clock clock;

    public SessionStore(Clock clock) {
        this.clock = clock;
    }

    public String create(SessionUser user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessions.put(token, new Entry(user, clock.instant()));
        return token;
    }

    public Optional<SessionUser> touch(String token) {
        Instant now = clock.instant();
        Entry e = sessions.computeIfPresent(token, (k, v) ->
            v.lastSeen().plus(IDLE_TIMEOUT).isBefore(now) ? null : new Entry(v.user(), now));
        return Optional.ofNullable(e).map(Entry::user);
    }

    public void remove(String token) {
        sessions.remove(token);
    }
}
