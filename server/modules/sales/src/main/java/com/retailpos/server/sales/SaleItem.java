package com.retailpos.server.sales;

import com.retailpos.server.catalog.ItemClass;
import com.retailpos.server.catalog.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Something sold. Name, price and tax are snapshots so history survives catalog edits. */
@Entity
@Table(name = "sale_item")
@DiscriminatorValue("SALE")
@PrimaryKeyJoinColumn(name = "basket_item_id")
public class SaleItem extends BasketItem {

    @Enumerated(EnumType.STRING)
    @Column(name = "item_class", nullable = false)
    private ItemClass itemClass;

    @Column(name = "non_merch_type")
    private String nonMerchType;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "discount_minor", nullable = false)
    private long discountMinor;

    /** Line discount in basis points (1000 = 10%); null = none. Kept so qty changes re-apply it. */
    @Column(name = "discount_bp")
    private Integer discountBp;

    @Column(name = "tax_rate_bp", nullable = false)
    private int taxRateBp;

    @Column(name = "tax_inclusive", nullable = false)
    private boolean taxInclusive;

    @Column(name = "tax_minor", nullable = false)
    private long taxMinor;

    @Column(name = "kitchen_sent_at")
    private Instant kitchenSentAt;

    private String note;

    @OneToMany(mappedBy = "saleItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItemModifier> modifiers = new ArrayList<>();

    protected SaleItem() {}

    SaleItem(PosTransaction txn, int lineNo, Product p, BigDecimal quantity, Instant now) {
        super(txn, lineNo, now);
        this.itemClass = p.getItemClass();
        this.nonMerchType = p.getNonMerchType();
        this.productId = p.getId();
        this.itemName = p.getName();
        this.unitPriceMinor = p.getPriceMinor();
        this.taxRateBp = p.effectiveTaxBp();
        this.taxInclusive = p.taxInclusive();
        setQuantity(quantity);
    }

    @Override
    public String kind() { return "SALE"; }

    void setQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.compareTo(BigDecimal.valueOf(9999)) > 0) {
            throw com.retailpos.server.common.PosException.badRequest("Quantity must be between 1 and 9999.");
        }
        this.quantity = quantity;
        price();
    }

    /** Sets (or with 0 clears) the line discount percent and re-prices the line. */
    void applyDiscount(int percent) {
        if (percent < 0 || percent > 100) throw com.retailpos.server.common.PosException.badRequest("Discount must be 0 to 100%.");
        this.discountBp = percent == 0 ? null : percent * 100;
        price();
    }

    /** (unit + modifiers) x qty before discount and before added tax, half-up to minor units. */
    public long grossMinor() {
        long unit = unitPriceMinor + modifiers.stream().mapToLong(SaleItemModifier::getPriceDeltaMinor).sum();
        return BigDecimal.valueOf(unit).multiply(quantity).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * gross = (unit + modifiers) x qty; discount = gross x percent (half-up); net = gross - discount.
     * Inclusive tax: tax = net x rate / (1 + rate), line amount = net.
     * Exclusive tax: tax = net x rate, line amount = net + tax.
     */
    void price() {
        long gross = grossMinor();
        if (discountBp != null) {
            this.discountMinor = BigDecimal.valueOf(gross).multiply(BigDecimal.valueOf(discountBp))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP).longValueExact();
        } else {
            this.discountMinor = 0;
        }
        long net = gross - discountMinor;
        BigDecimal rate = BigDecimal.valueOf(taxRateBp);
        BigDecimal divisor = taxInclusive ? rate.add(BigDecimal.valueOf(10_000)) : BigDecimal.valueOf(10_000);
        this.taxMinor = BigDecimal.valueOf(net).multiply(rate).divide(divisor, 0, RoundingMode.HALF_UP).longValueExact();
        this.amountMinor = taxInclusive ? net : net + taxMinor;
    }

    public boolean isMerchandise() { return itemClass == ItemClass.MERCHANDISE; }
    public ItemClass getItemClass() { return itemClass; }
    public UUID getProductId() { return productId; }
    public String getItemName() { return itemName; }
    public long getUnitPriceMinor() { return unitPriceMinor; }
    public BigDecimal getQuantity() { return quantity; }
    public long getTaxMinor() { return taxMinor; }
    public long getDiscountMinor() { return discountMinor; }
    /** Whole percent, or 0 when there is no discount. */
    public int getDiscountPercent() { return discountBp == null ? 0 : discountBp / 100; }
    public List<SaleItemModifier> getModifiers() { return modifiers; }
}
