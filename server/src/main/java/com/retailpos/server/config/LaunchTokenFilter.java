package com.retailpos.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Only the Electron instance that launched this server may call /api: it passes a random
 * per-launch token (env POS_LAUNCH_TOKEN) that the renderer sends as X-POS-Launch-Token.
 * When no token is configured (plain dev server), the check is off.
 */
public class LaunchTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-POS-Launch-Token";

    private final byte[] expected;

    public LaunchTokenFilter(String launchToken) {
        this.expected = launchToken == null || launchToken.isBlank() ? null : launchToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return expected == null || !req.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String given = req.getHeader(HEADER);
        if (given == null || !MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid launch token");
            return;
        }
        chain.doFilter(req, res);
    }
}
