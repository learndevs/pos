package com.retailpos.server.sales;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One line in a transaction's basket. kind = SALE (SaleItem) or TENDER (TenderItem);
 * each subtype has its own table joined on basket_item_id.
 */
@Entity
@Table(name = "basket_item")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "kind")
public abstract class BasketItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private PosTransaction transaction;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** SALE: line total. TENDER: amount applied to the bill. Always positive. */
    @Column(name = "amount_minor", nullable = false)
    protected long amountMinor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BasketItem() {}

    protected BasketItem(PosTransaction transaction, int lineNo, Instant now) {
        super(true);
        this.transaction = transaction;
        this.lineNo = lineNo;
        this.createdAt = now;
    }

    public abstract String kind();

    public PosTransaction getTransaction() { return transaction; }
    public int getLineNo() { return lineNo; }
    public long getAmountMinor() { return amountMinor; }
}
