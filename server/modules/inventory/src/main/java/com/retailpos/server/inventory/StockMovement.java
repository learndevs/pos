package com.retailpos.server.inventory;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Stock ledger row (append-only). Quantity on hand = sum of quantity_delta per stock item. */
@Entity
@Table(name = "stock_movement")
public class StockMovement extends BaseEntity {

    @Column(name = "stock_item_id", nullable = false, updatable = false)
    private UUID stockItemId;

    /** OPENING, PURCHASE, SALE, SALE_REVERSAL, ADJUSTMENT, WASTE */
    @Column(nullable = false, updatable = false)
    private String type;

    @Column(name = "quantity_delta", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantityDelta;

    @Column(updatable = false)
    private String reason;

    @Column(name = "sale_item_id", updatable = false)
    private UUID saleItemId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {}

    public static StockMovement sale(UUID stockItemId, BigDecimal quantity, UUID saleItemId, UUID userId,
                                     LocalDate businessDate, Instant now) {
        StockMovement m = new StockMovement(true);
        m.stockItemId = stockItemId;
        m.type = "SALE";
        m.quantityDelta = quantity.negate();
        m.saleItemId = saleItemId;
        m.userId = userId;
        m.businessDate = businessDate;
        m.createdAt = now;
        return m;
    }

    private StockMovement(boolean newRow) {
        super(newRow);
    }

    public UUID getStockItemId() { return stockItemId; }
    public BigDecimal getQuantityDelta() { return quantityDelta; }
}
