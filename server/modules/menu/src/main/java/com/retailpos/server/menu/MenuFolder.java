package com.retailpos.server.menu;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Node in a menu's folder tree. parentFolderId null = the menu's root. */
@Entity
@Table(name = "menu_folder")
public class MenuFolder extends BaseEntity {

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(nullable = false)
    private String name;

    protected MenuFolder() {}

    public boolean isRoot() { return parentFolderId == null; }
    public UUID getMenuId() { return menuId; }
    public UUID getParentFolderId() { return parentFolderId; }
    public String getName() { return name; }
}
