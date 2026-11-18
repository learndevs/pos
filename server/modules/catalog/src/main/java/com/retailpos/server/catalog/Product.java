package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
import com.retailpos.server.common.PosException;
import java.util.UUID;
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

    private Product(UUID id) {
        super(id);
    }

    /** All editable product fields; used by admin edits and by sync on the till (same rules both sides). */
    public record Data(Category category, TaxRate taxRate, String name, ItemClass itemClass, String nonMerchType,
                       String sku, String barcode, long priceMinor, boolean kitchenPrint, boolean trackStock,
                       int sortOrder, boolean active) {}

    public static Product create(UUID id, Data d, Instant now) {
        Product p = id == null ? new Product(com.retailpos.server.common.UuidV7.next()) : new Product(id);
        p.createdAt = now;
        p.update(d, now);
        return p;
    }

    public void update(Data d, Instant now) {
        if (d.category() == null) throw PosException.badRequest("Choose a category.");
        if (d.name() == null || d.name().isBlank()) throw PosException.badRequest("Product name is required.");
        if (d.name().length() > 120) throw PosException.badRequest("Product name is too long (max 120).");
        if (d.priceMinor() < 0 || d.priceMinor() > 1_000_000_000L) throw PosException.badRequest("Price is out of range.");
        if (d.itemClass() == null) throw PosException.badRequest("Choose merchandise or non-merchandise.");
        boolean nonMerch = d.itemClass() == ItemClass.NON_MERCHANDISE;
        if (nonMerch && (d.nonMerchType() == null || d.nonMerchType().isBlank())) {
            throw PosException.badRequest("Non-merchandise needs a type (service charge, delivery, ...).");
        }
        if (nonMerch && d.trackStock()) throw PosException.badRequest("Only merchandise can track stock.");
        this.category = d.category();
        this.taxRate = d.taxRate();
        this.name = d.name().trim();
        this.itemClass = d.itemClass();
        this.nonMerchType = nonMerch ? d.nonMerchType() : null;
        this.sku = blankToNull(d.sku());
        this.barcode = blankToNull(d.barcode());
        this.priceMinor = d.priceMinor();
        this.kitchenPrint = d.kitchenPrint();
        this.trackStock = d.trackStock();
        this.sortOrder = d.sortOrder();
        this.active = d.active();
        this.updatedAt = now;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

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
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
}
