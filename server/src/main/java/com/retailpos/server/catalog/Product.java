package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "product")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_class", nullable = false)
    private ItemClass itemClass;

    /** SERVICE_CHARGE, DELIVERY, TIP, DEPOSIT, GIFT_CARD, OTHER (non-merchandise only). */
    @Column(name = "non_merch_type")
    private String nonMerchType;

    private String sku;
    private String barcode;

    @Column(name = "price_minor", nullable = false)
    private long priceMinor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRate;

    @Column(name = "kitchen_print", nullable = false)
    private boolean kitchenPrint;

    @Column(name = "track_stock", nullable = false)
    private boolean trackStock;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {}

    /** Tax rate in basis points if the rate is active, else 0. */
    public int effectiveTaxBp() {
        return taxRate != null && taxRate.isActive() ? taxRate.getRateBp() : 0;
    }

    public boolean taxInclusive() {
        return taxRate == null || taxRate.isInclusive();
    }

    public Category getCategory() { return category; }
    public String getName() { return name; }
    public ItemClass getItemClass() { return itemClass; }
    public String getNonMerchType() { return nonMerchType; }
    public String getSku() { return sku; }
    public String getBarcode() { return barcode; }
    public long getPriceMinor() { return priceMinor; }
    public TaxRate getTaxRate() { return taxRate; }
    public boolean isKitchenPrint() { return kitchenPrint; }
    public boolean isTrackStock() { return trackStock; }
    public boolean isActive() { return active; }
}
