package com.retailpos.server.sales;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** A modifier chosen for a sale item (e.g. + Cheese). Snapshot of name and price. */
@Entity
@Table(name = "sale_item_modifier")
public class SaleItemModifier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;

    @Column(name = "modifier_id")
    private UUID modifierId;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_delta_minor", nullable = false)
    private long priceDeltaMinor;

    protected SaleItemModifier() {}

    public String getName() { return name; }
    public long getPriceDeltaMinor() { return priceDeltaMinor; }
}
