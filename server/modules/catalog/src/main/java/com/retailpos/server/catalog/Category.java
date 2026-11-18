package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
import com.retailpos.server.common.PosException;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    protected Category() {}

    private Category(UUID id) {
        super(id);
    }

    public static Category create(UUID id, String name, int sortOrder, boolean active) {
        Category c = id == null ? new Category(com.retailpos.server.common.UuidV7.next()) : new Category(id);
        c.update(name, sortOrder, active);
        return c;
    }

    public void update(String name, int sortOrder, boolean active) {
        if (name == null || name.isBlank()) throw PosException.badRequest("Category name is required.");
        this.name = name.trim();
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
}
