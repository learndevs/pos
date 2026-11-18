package com.retailpos.server.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    static final String PIN_PATTERN = "\\d{4,8}";

    public record LoginRequest(@NotBlank @Size(max = 50) String username,
                               @NotBlank @Pattern(regexp = PIN_PATTERN) String pin) {}

    public record OwnerRequest(@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
                               @NotBlank @Size(max = 80) String displayName,
                               @NotBlank @Pattern(regexp = PIN_PATTERN) String pin) {}

    public record SetupStatus(boolean needsOwner) {}

    public record UserTile(UUID id, String username, String displayName) {}

    private final AuthService auth;
    private final AppUserRepository users;

    public AuthController(AuthService auth, AppUserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @GetMapping("/setup/status")
    public SetupStatus setupStatus() {
        return new SetupStatus(auth.needsOwner());
    }

    @PostMapping("/setup/owner")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionUser createOwner(@Valid @RequestBody OwnerRequest req) {
        return auth.createOwner(req.username(), req.displayName(), req.pin());
    }

    /** Names for the sign-in tiles. Local, loopback-only API, so listing names is acceptable. */
    @GetMapping("/auth/users")
    public List<UserTile> users() {
        return users.findByActiveTrueOrderByDisplayName().stream()
            .map(u -> new UserTile(u.getId(), u.getUsername(), u.getDisplayName()))
            .toList();
    }

    @PostMapping("/auth/login")
    public AuthService.LoginResult login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.username(), req.pin());
    }

    @GetMapping("/auth/me")
    public SessionUser me(@AuthenticationPrincipal SessionUser user) {
        return user;
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        auth.logout(SessionAuthFilter.bearer(authorization));
    }
}
