package com.retailpos.server.identity;

import java.util.List;
import java.util.UUID;

/** The signed-in user as the API and UI see it. */
public record SessionUser(UUID id, String username, String displayName, String role, List<String> permissions) {}
