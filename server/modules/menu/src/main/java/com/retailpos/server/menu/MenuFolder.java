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

    @Column(nullable = false)
    private boolean active = true;

    protected MenuFolder() {}

    private MenuFolder(UUID id) {
        super(id);
    }

    /** New sub-folder (root folders are seeded per menu). id null = generate (admin) or the synced id (till). */
    public static MenuFolder create(UUID id, UUID menuId, UUID parentFolderId, String name, boolean active) {
        if (parentFolderId == null) throw com.retailpos.server.common.PosException.badRequest("A folder needs a parent folder.");
        MenuFolder f = new MenuFolder(id == null ? com.retailpos.server.common.UuidV7.next() : id);
        f.menuId = menuId;
        f.parentFolderId = parentFolderId;
        f.update(name, active);
        return f;
    }

    public void update(String name, boolean active) {
        if (name == null || name.isBlank()) throw com.retailpos.server.common.PosException.badRequest("Folder name is required.");
        if (name.length() > 40) throw com.retailpos.server.common.PosException.badRequest("Folder name is too long (max 40).");
        if (isRoot() && !active) throw com.retailpos.server.common.PosException.badRequest("A menu's root folder cannot be removed.");
        this.name = name.trim();
        this.active = active;
    }

    public boolean isRoot() { return parentFolderId == null; }
    public UUID getMenuId() { return menuId; }
    public UUID getParentFolderId() { return parentFolderId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
