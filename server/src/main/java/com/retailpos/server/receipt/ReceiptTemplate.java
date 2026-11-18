package com.retailpos.server.receipt;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** Receipt layout as HTML with Mustache placeholders. Exactly one is active. */
@Entity
@Table(name = "receipt_template")
public class ReceiptTemplate extends BaseEntity {

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String html;
    @Column(name = "paper_width_mm", nullable = false) private int paperWidthMm;
    @Column(nullable = false) private boolean active;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ReceiptTemplate() {}

    void update(String name, String html, int paperWidthMm, Instant now) {
        this.name = name;
        this.html = html;
        this.paperWidthMm = paperWidthMm;
        this.updatedAt = now;
    }

    public String getName() { return name; }
    public String getHtml() { return html; }
    public int getPaperWidthMm() { return paperWidthMm; }
    public boolean isActive() { return active; }
}
