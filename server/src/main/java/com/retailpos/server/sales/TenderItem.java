package com.retailpos.server.sales;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;

/** Money received. amount_minor (base row) = amount applied to the bill; cash may include change. */
@Entity
@Table(name = "tender_item")
@DiscriminatorValue("TENDER")
@PrimaryKeyJoinColumn(name = "basket_item_id")
public class TenderItem extends BasketItem {

    public enum Method { CASH, CARD, OTHER }

    @Column(nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Method method;

    @Column(name = "tendered_minor")
    private Long tenderedMinor;

    @Column(name = "change_minor")
    private Long changeMinor;

    private String reference;

    protected TenderItem() {}

    TenderItem(PosTransaction txn, int lineNo, Method method, long applied, Long tendered, Long change, Instant now) {
        super(txn, lineNo, now);
        this.method = method;
        this.amountMinor = applied;
        this.tenderedMinor = tendered;
        this.changeMinor = change;
    }

    @Override
    public String kind() { return "TENDER"; }

    public Method getMethod() { return method; }
    public Long getTenderedMinor() { return tenderedMinor; }
    public Long getChangeMinor() { return changeMinor; }
}
