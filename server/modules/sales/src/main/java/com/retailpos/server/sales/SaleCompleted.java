package com.retailpos.server.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published by SalesService inside the completion transaction, before the sale is marked COMPLETED.
 * Listeners (e.g. inventory) run synchronously in the same DB transaction: if one fails, the sale
 * does not complete.
 */
public record SaleCompleted(UUID transactionId, UUID userId, LocalDate businessDate, List<SoldItem> items) {

    /** A merchandise line; trackStock comes from the product at completion time. */
    public record SoldItem(UUID saleItemId, UUID productId, BigDecimal quantity, boolean trackStock) {}
}
