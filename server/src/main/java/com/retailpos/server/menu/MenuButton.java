package com.retailpos.server.menu;

import com.retailpos.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** One slot of the 3 x 4 grid (page, row 0-2, col 0-3) in a folder, and the action it triggers. */
@Entity
@Table(name = "menu_button")
public class MenuButton extends BaseEntity {

    @Column(name = "folder_id", nullable = false) private UUID folderId;
    @Column(nullable = false) private int page;
    @Column(name = "grid_row", nullable = false) private int gridRow;
    @Column(name = "grid_col", nullable = false) private int gridCol;
    @Column(nullable = false) private String label;
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "product_id") private UUID productId;
    @Column(name = "target_folder_id") private UUID targetFolderId;
    @Column(name = "tender_method") private String tenderMethod;
    @Column(name = "amount_minor") private Long amountMinor;
    @Column(name = "function_code") private String functionCode;
    @Column(name = "target_menu_id") private UUID targetMenuId;
    @Column(nullable = false) private boolean active;

    protected MenuButton() {}

    public UUID getFolderId() { return folderId; }
    public int getPage() { return page; }
    public int getGridRow() { return gridRow; }
    public int getGridCol() { return gridCol; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
    public ActionType getActionType() { return actionType; }
    public UUID getProductId() { return productId; }
    public UUID getTargetFolderId() { return targetFolderId; }
    public String getTenderMethod() { return tenderMethod; }
    public Long getAmountMinor() { return amountMinor; }
    public String getFunctionCode() { return functionCode; }
    public UUID getTargetMenuId() { return targetMenuId; }
    public boolean isActive() { return active; }
}
