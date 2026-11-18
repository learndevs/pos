package com.retailpos.server.catalog;

import com.retailpos.server.common.BaseEntity;
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

    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
}
