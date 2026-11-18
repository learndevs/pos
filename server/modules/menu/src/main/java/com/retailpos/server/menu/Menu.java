package com.retailpos.server.menu;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** SALE, TENDER or ITEM. Each has one root folder. */
@Entity
@Table(name = "menu")
public class Menu extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Menu() {}

    public String getCode() { return code; }
    public String getName() { return name; }
}
