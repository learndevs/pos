package com.retailpos.server.admin;

import com.retailpos.server.common.PosException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/** Admin accounts: first-run setup, password login, in-memory sessions (Bearer token). */
public final class AdminAuth {

    private AdminAuth() {}

    public record Principal(UUID id, String username, String displayName) {}

    interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
        Optional<AdminUser> findByUsernameIgnoreCase(String username);
    }

    @Component
    static class Sessions {
        private static final Duration IDLE = Duration.ofHours(8);
        private record Entry(Principal user, Instant lastSeen) {}
        private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
        private final SecureRandom random = new SecureRandom();
        private final Clock clock;

        Sessions(Clock clock) { this.clock = clock; }

        String create(Principal p) {
            byte[] b = new byte[32];
            random.nextBytes(b);
            String t = HexFormat.of().formatHex(b);
            sessions.put(t, new Entry(p, clock.instant()));
            return t;
        }

        Optional<Principal> touch(String token) {
            Instant now = clock.instant();
            Entry e = sessions.computeIfPresent(token, (k, v) -> v.lastSeen().plus(IDLE).isBefore(now) ? null : new Entry(v.user(), now));
            return Optional.ofNullable(e).map(Entry::user);
        }

        void remove(String token) { sessions.remove(token); }
    }

    /** Resolves "Authorization: Bearer <admin session>" for /admin/api/**. */
    static class SessionFilter extends OncePerRequestFilter {
        private final Sessions sessions;
        SessionFilter(Sessions sessions) { this.sessions = sessions; }

        static String bearer(String h) { return h != null && h.startsWith("Bearer ") ? h.substring(7).trim() : null; }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest req) { return !req.getRequestURI().startsWith("/admin/api/"); }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
            String token = bearer(req.getHeader(HttpHeaders.AUTHORIZATION));
            if (token != null) {
                sessions.touch(token).ifPresent(p -> SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(p, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))));
            }
            chain.doFilter(req, res);
        }
    }

    @Service
    @Transactional
    static class AuthService {
        private final AdminUserRepository users;
        private final PasswordEncoder encoder;
        private final Sessions sessions;
        private final Clock clock;

        AuthService(AdminUserRepository users, PasswordEncoder encoder, Sessions sessions, Clock clock) {
            this.users = users; this.encoder = encoder; this.sessions = sessions; this.clock = clock;
        }

        @Transactional(readOnly = true)
        boolean needsSetup() { return users.count() == 0; }

        Principal setup(String username, String displayName, String password) {
            if (!needsSetup()) throw PosException.conflict("Setup is already complete.");
            AdminUser u = users.save(new AdminUser(username.trim(), displayName.trim(), encoder.encode(password), clock.instant()));
            return new Principal(u.getId(), u.getUsername(), u.getDisplayName());
        }

        @Transactional(readOnly = true)
        Map<String, Object> login(String username, String password) {
            AdminUser u = users.findByUsernameIgnoreCase(username.trim()).filter(AdminUser::isActive)
                .filter(x -> encoder.matches(password, x.getPasswordHash()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect username or password."));
            Principal p = new Principal(u.getId(), u.getUsername(), u.getDisplayName());
            return Map.of("token", sessions.create(p), "user", p);
        }
    }

    @RestController
    @RequestMapping("/admin/api")
    static class AuthController {
        record Setup(@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
                     @NotBlank @Size(max = 80) String displayName, @NotBlank @Size(min = 8, max = 100) String password) {}
        record Login(@NotBlank String username, @NotBlank String password) {}

        private final AuthService auth;
        private final Sessions sessions;
        AuthController(AuthService auth, Sessions sessions) { this.auth = auth; this.sessions = sessions; }

        @GetMapping("/setup/status")
        Map<String, Boolean> setupStatus() { return Map.of("needsSetup", auth.needsSetup()); }

        @PostMapping("/setup")
        @ResponseStatus(HttpStatus.CREATED)
        Principal setup(@Valid @RequestBody Setup req) { return auth.setup(req.username(), req.displayName(), req.password()); }

        @PostMapping("/login")
        Map<String, Object> login(@Valid @RequestBody Login req) { return auth.login(req.username(), req.password()); }

        @GetMapping("/me")
        Principal me(@AuthenticationPrincipal Principal p) { return p; }

        @PostMapping("/logout")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void logout(jakarta.servlet.http.HttpServletRequest req) {
            String t = SessionFilter.bearer(req.getHeader(HttpHeaders.AUTHORIZATION));
            if (t != null) sessions.remove(t);
        }
    }
}
