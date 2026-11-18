package com.retailpos.server.sync.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Terminal registration and token checks. The plain token is shown once, at creation. */
@Service
@Transactional
public class Terminals {

    public record Created(Terminal terminal, String token) {}

    private final TerminalRepository terminals;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    Terminals(TerminalRepository terminals, Clock clock) {
        this.terminals = terminals;
        this.clock = clock;
    }

    public Created create(String name) {
        if (name == null || name.isBlank()) throw com.retailpos.server.common.PosException.badRequest("Terminal name is required.");
        byte[] b = new byte[24];
        random.nextBytes(b);
        String token = HexFormat.of().formatHex(b);
        return new Created(terminals.save(new Terminal(name.trim(), sha256(token), clock.instant())), token);
    }

    @Transactional(readOnly = true)
    public List<Terminal> list() {
        return terminals.findAllByOrderByNameAsc();
    }

    /** Valid active terminal for this token, and records that it was seen. */
    public Optional<Terminal> authenticate(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return terminals.findByTokenSha256AndActiveTrue(sha256(token)).map(t -> {
            t.seen(clock.instant());
            return t;
        });
    }

    @Transactional(readOnly = true)
    public List<Terminal> byIds(List<java.util.UUID> ids) {
        return terminals.findAllById(ids);
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
