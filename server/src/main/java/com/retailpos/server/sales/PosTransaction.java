package com.retailpos.server.sales;

import com.retailpos.server.catalog.Product;
import com.retailpos.server.common.BaseEntity;
import com.retailpos.server.common.PosException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A sale (or refund) and its basket. Aggregate root: all changes to basket items go through
 * this class so totals stay consistent. Editable only while OPEN (the DB enforces it too).
 */
@Entity
@Table(name = "pos_transaction")
public class PosTransaction extends BaseEntity {

    public static final String OPEN = "OPEN", COMPLETED = "COMPLETED", CANCELLED = "CANCELLED";

    @Column(nullable = false, updatable = false)
    private String type;                 // SALE | REFUND

    @Column(nullable = false)
    private String status;

    @Column(name = "cash_session_id", nullable = false, updatable = false)
    private UUID cashSessionId;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "order_type", nullable = false)
    private String orderType;            // DINE_IN | TAKEAWAY

    @Column(name = "table_id")
    private UUID tableId;

    @Column(name = "original_transaction_id", updatable = false)
    private UUID originalTransactionId;

    @Column(name = "subtotal_minor", nullable = false) private long subtotalMinor;
    @Column(name = "discount_minor", nullable = false) private long discountMinor;
    @Column(name = "discount_reason") private String discountReason;
    @Column(name = "tax_minor", nullable = false) private long taxMinor;
    @Column(name = "rounding_minor", nullable = false) private long roundingMinor;
    @Column(name = "total_minor", nullable = false) private long totalMinor;

    private String note;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "cancelled_by") private UUID cancelledBy;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    private String reason;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<BasketItem> items = new ArrayList<>();

    protected PosTransaction() {}

    private PosTransaction(boolean newRow) {
        super(newRow);
    }

    public static PosTransaction startSale(UUID cashSessionId, LocalDate businessDate, UUID userId, Instant now) {
        PosTransaction t = new PosTransaction(true);
        t.type = "SALE";
        t.status = OPEN;
        t.cashSessionId = cashSessionId;
        t.businessDate = businessDate;
        t.orderType = "TAKEAWAY";
        t.createdBy = userId;
        t.createdAt = now;
        return t;
    }

    // ---------- basket ----------

    public SaleItem addSaleItem(Product product, BigDecimal quantity, Instant now) {
        requireOpen();
        SaleItem item = new SaleItem(this, nextLineNo(), product, quantity, now);
        items.add(item);
        recalc();
        return item;
    }

    public void setQuantity(UUID itemId, BigDecimal quantity) {
        requireOpen();
        BasketItem item = line(itemId);
        if (!(item instanceof SaleItem sale)) throw PosException.badRequest("Select an item line, not a payment.");
        sale.setQuantity(quantity);
        recalc();
    }

    /** Removes a sale or tender line while OPEN. Finished sales need a refund instead. */
    public void removeLine(UUID itemId) {
        requireOpen();
        items.remove(line(itemId));
        recalc();
    }

    public void setOrderType(String orderType) {
        requireOpen();
        this.orderType = orderType;
    }

    // ---------- payment ----------

    /** Adds a tender. amount null = exact amount due. Cash may exceed the due amount (change). */
    public TenderItem addTender(TenderItem.Method method, Long amount, Instant now) {
        requireOpen();
        if (totalMinor == 0) throw PosException.conflict("Add items before taking payment.");
        long due = dueMinor();
        if (due <= 0) throw PosException.conflict("Nothing left to pay.");
        long given = amount == null ? due : amount;
        if (given <= 0) throw PosException.badRequest("Amount must be more than zero.");

        TenderItem tender;
        if (method == TenderItem.Method.CASH) {
            long applied = Math.min(given, due);
            tender = new TenderItem(this, nextLineNo(), method, applied, given, given - applied, now);
        } else {
            if (given > due) throw PosException.badRequest("Card or other amount cannot exceed the amount due.");
            tender = new TenderItem(this, nextLineNo(), method, given, null, null, now);
        }
        items.add(tender);
        return tender;
    }

    public boolean isFullyPaid() {
        return totalMinor > 0 && dueMinor() == 0;
    }

    void complete(String receiptNo, Instant now) {
        requireOpen();
        if (!isFullyPaid()) throw PosException.conflict("The sale is not fully paid.");
        this.receiptNo = receiptNo;
        this.completedAt = now;
        this.status = COMPLETED;
    }

    public void cancel(UUID userId, String reason, Instant now) {
        requireOpen();
        this.status = CANCELLED;
        this.cancelledBy = userId;
        this.cancelledAt = now;
        this.reason = reason;
    }

    // ---------- derived ----------

    public long paidMinor() {
        return items.stream().filter(i -> i instanceof TenderItem).mapToLong(BasketItem::getAmountMinor).sum();
    }

    public long dueMinor() {
        return Math.max(0, totalMinor - paidMinor());
    }

    public long changeMinor() {
        return items.stream().filter(i -> i instanceof TenderItem t && t.getChangeMinor() != null)
            .mapToLong(i -> ((TenderItem) i).getChangeMinor()).sum();
    }

    public List<SaleItem> saleItems() {
        return items.stream().filter(SaleItem.class::isInstance).map(SaleItem.class::cast).toList();
    }

    private void recalc() {
        subtotalMinor = saleItems().stream().mapToLong(BasketItem::getAmountMinor).sum();
        taxMinor = saleItems().stream().mapToLong(SaleItem::getTaxMinor).sum();
        totalMinor = subtotalMinor - discountMinor + roundingMinor;
    }

    private int nextLineNo() {
        return items.stream().mapToInt(BasketItem::getLineNo).max().orElse(0) + 1;
    }

    private BasketItem line(UUID itemId) {
        return items.stream().filter(i -> i.getId().equals(itemId)).findFirst()
            .orElseThrow(() -> PosException.notFound("Line not found."));
    }

    private void requireOpen() {
        if (!OPEN.equals(status)) throw PosException.conflict("This sale is already " + status.toLowerCase() + ".");
    }

    public String getType() { return type; }
    public String getStatus() { return status; }
    public UUID getCashSessionId() { return cashSessionId; }
    public LocalDate getBusinessDate() { return businessDate; }
    public String getReceiptNo() { return receiptNo; }
    public String getOrderType() { return orderType; }
    public long getSubtotalMinor() { return subtotalMinor; }
    public long getTaxMinor() { return taxMinor; }
    public long getTotalMinor() { return totalMinor; }
    public UUID getCreatedBy() { return createdBy; }
    public List<BasketItem> getItems() { return items; }
}
