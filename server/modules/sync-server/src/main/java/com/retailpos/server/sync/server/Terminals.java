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

    public record Next(int tillNo, String code) {}

    /** Suggested number + code for a new till (the create form pre-fills these). */
    @Transactional(readOnly = true)
    public Next next() {
        int n = terminals.findAll().stream().mapToInt(Terminal::getTillNo).max().orElse(0) + 1;
        return new Next(n, defaultCode(n));
    }

    /** New till. tillNo / code null = next free. The plain token is returned once. */
    public Created create(String name, Integer tillNo, String code, String location) {
        Next next = next();
        int no = tillNo == null ? next.tillNo() : tillNo;
        String c = code == null || code.isBlank() ? defaultCode(no) : code;
        validate(null, no, name, c);
        String token = newToken();
        return new Created(terminals.save(new Terminal(no, name.trim(), c.trim().toUpperCase(), blankToNull(location), sha256(token), clock.instant())), token);
    }

    public Terminal update(java.util.UUID id, Integer tillNo, String name, String code, String location) {
        Terminal t = get(id);
        int no = tillNo == null ? t.getTillNo() : tillNo;
        validate(id, no, name, code);
        t.update(no, name.trim(), code.trim().toUpperCase(), blankToNull(location));
        return t;
    }

    private void validate(java.util.UUID self, int tillNo, String name, String code) {
        var bad = (java.util.function.Function<String, RuntimeException>) com.retailpos.server.common.PosException::badRequest;
        if (name == null || name.isBlank()) throw bad.apply("Till name is required.");
        if (tillNo < 1 || tillNo > 999) throw bad.apply("Till number must be 1 to 999.");
        String c = code == null ? "" : code.trim().toUpperCase();
        if (!c.matches("[A-Z0-9]{1,6}")) throw bad.apply("Device code: 1-6 letters or digits, e.g. T01.");
        for (Terminal o : terminals.findAll()) {
            if (o.getId().equals(self)) continue;
            if (o.getTillNo() == tillNo) throw com.retailpos.server.common.PosException.conflict("Till number " + tillNo + " is already used by " + o.getName() + ".");
            if (o.getCode().equals(c)) throw com.retailpos.server.common.PosException.conflict("Code " + c + " is already used by " + o.getName() + ".");
        }
    }

    static String defaultCode(int tillNo) {
        return "T" + (tillNo < 10 ? "0" + tillNo : String.valueOf(tillNo));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Issues a new token (the old one stops working) and re-enables the till. */
    public Created resetToken(java.util.UUID id) {
        Terminal t = get(id);
        String token = newToken();
        t.newToken(sha256(token));
        return new Created(t, token);
    }

    private Terminal get(java.util.UUID id) {
        return terminals.findById(id).orElseThrow(() -> com.retailpos.server.common.PosException.notFound("Terminal not found."));
    }

    private String newToken() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    @Transactional(readOnly = true)
    public List<Terminal> list() {
        return terminals.findAllByOrderByNameAsc();
    }

    /** Revokes the till's token (it can no longer fetch packages or upload). */
    public void disable(java.util.UUID id) {
        terminals.findById(id).orElseThrow(() -> com.retailpos.server.common.PosException.notFound("Terminal not found.")).disable();
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
