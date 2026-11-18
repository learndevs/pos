package com.retailpos.server.sales;

import java.util.UUID;

/**
 * Published inside the DB transaction that finishes a sale (COMPLETED or CANCELLED), after the status change.
 * Listeners (e.g. sync-client's archive outbox) run in that same transaction.
 */
public record TransactionFinished(UUID transactionId, String status) {}
