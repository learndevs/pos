package com.retailpos.server.identity;

import com.retailpos.server.audit.AuditLog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_FOR = Duration.ofMinutes(5);

    public record LoginResult(String token, SessionUser user) {}

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final SessionStore sessions;
    private final AuditLog audit;
    private final Clock clock;

    public AuthService(AppUserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       SessionStore sessions, AuditLog audit, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    // Not rolled back on 401/423: failed attempts and lockouts must persist.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResult login(String username, String pin) {
        Instant now = clock.instant();
        AppUser user = users.findByUsernameIgnoreCase(username.trim())
            .filter(AppUser::isActive)
            .orElseThrow(() -> {
                audit.record(null, "LOGIN_FAIL", "app_user", null, "{\"reason\":\"unknown user\"}");
                return invalid();
            });

        if (user.isLocked(now)) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempts. Try again later.");
        }
        if (!encoder.matches(pin, user.getPinHash())) {
            user.recordFailure(now, MAX_ATTEMPTS, LOCK_FOR);
            boolean nowLocked = user.isLocked(now);
            audit.record(user.getId(), nowLocked ? "LOGIN_LOCKED" : "LOGIN_FAIL", "app_user", user.getId(), null);
            if (nowLocked) {
                throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempts. Try again later.");
            }
            throw invalid();
        }

        user.recordSuccess(now);
        audit.record(user.getId(), "LOGIN", "app_user", user.getId(), null);
        SessionUser session = toSessionUser(user);
        return new LoginResult(sessions.create(session), session);
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    @Transactional(readOnly = true)
    public boolean needsOwner() {
        return users.count() == 0;
    }

    /** First-run only: creates the owner account while no users exist. */
    @Transactional
    public SessionUser createOwner(String username, String displayName, String pin) {
        if (!needsOwner()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup is already complete.");
        }
        Role owner = roles.findByCode(Role.OWNER).orElseThrow();
        // flush now: AuditLog writes via JDBC and references this row
        AppUser user = users.saveAndFlush(new AppUser(username.trim(), displayName.trim(), encoder.encode(pin), owner, clock.instant()));
        audit.record(user.getId(), "SETUP_OWNER", "app_user", user.getId(), null);
        return toSessionUser(user);
    }

    private SessionUser toSessionUser(AppUser u) {
        return new SessionUser(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().getCode(),
            roles.findPermissions(u.getRole().getId()));
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect user or PIN.");
    }
}
