package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tax_rate")
public class TaxRate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Basis points: 1000 = 10%. */
    @Column(name = "rate_bp", nullable = false)
    private int rateBp;

    /** true = prices include tax; false = tax is added on top. */
    @Column(nullable = false)
    private boolean inclusive;

    @Column(nullable = false)
    private boolean active;

    protected TaxRate() {}

    public String getName() { return name; }
    public int getRateBp() { return rateBp; }
    public boolean isInclusive() { return inclusive; }
    public boolean isActive() { return active; }
}
