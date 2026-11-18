package com.retailpos.server.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Business-rule failure with a message safe to show at the till (returned as RFC 7807 detail). */
public class PosException extends ResponseStatusException {

    private PosException(HttpStatus status, String message) {
        super(status, message);
    }

    public static PosException conflict(String message) { return new PosException(HttpStatus.CONFLICT, message); }
    public static PosException badRequest(String message) { return new PosException(HttpStatus.BAD_REQUEST, message); }
    public static PosException notFound(String message) { return new PosException(HttpStatus.NOT_FOUND, message); }
}
