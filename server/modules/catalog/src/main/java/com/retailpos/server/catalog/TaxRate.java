package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
import com.retailpos.server.common.PosException;
import java.util.UUID;
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

    private TaxRate(UUID id) {
        super(id);
    }

    /** New tax rate; id null = generate (admin), or the synced id (till). */
    public static TaxRate create(UUID id, String name, int rateBp, boolean inclusive, boolean active) {
        TaxRate t = id == null ? new TaxRate(com.retailpos.server.common.UuidV7.next()) : new TaxRate(id);
        t.update(name, rateBp, inclusive, active);
        return t;
    }

    public void update(String name, int rateBp, boolean inclusive, boolean active) {
        if (name == null || name.isBlank()) throw PosException.badRequest("Tax rate name is required.");
        if (rateBp < 0 || rateBp > 10_000) throw PosException.badRequest("Tax rate must be 0 to 100%.");
        this.name = name.trim();
        this.rateBp = rateBp;
        this.inclusive = inclusive;
        this.active = active;
    }

    public String getName() { return name; }
    public int getRateBp() { return rateBp; }
    public boolean isInclusive() { return inclusive; }
    public boolean isActive() { return active; }
}
