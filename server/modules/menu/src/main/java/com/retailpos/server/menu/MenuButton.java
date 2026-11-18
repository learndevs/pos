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

    private MenuButton(UUID id) {
        super(id);
    }

    /** Everything a button is: slot + look + exactly one action target (matching action type). */
    public record Data(UUID folderId, int page, int gridRow, int gridCol, String label, String color, ActionType actionType,
                       UUID productId, UUID targetFolderId, String tenderMethod, Long amountMinor, String functionCode,
                       UUID targetMenuId, boolean active) {}

    public static MenuButton create(UUID id, Data d) {
        MenuButton b = new MenuButton(id == null ? com.retailpos.server.common.UuidV7.next() : id);
        b.update(d);
        return b;
    }

    /** Same rules on admin and till (the DB CHECKs mirror them). */
    public void update(Data d) {
        var bad = (java.util.function.Function<String, RuntimeException>) com.retailpos.server.common.PosException::badRequest;
        if (d.folderId() == null) throw bad.apply("A button needs a folder.");
        if (d.page() < 0 || d.page() > 49) throw bad.apply("Page must be 1 to 50.");
        if (d.gridRow() < 0 || d.gridRow() > 3 || d.gridCol() < 0 || d.gridCol() > 2) throw bad.apply("The grid is 4 rows x 3 columns.");
        if (d.label() == null || d.label().isBlank()) throw bad.apply("Button label is required.");
        if (d.label().length() > 40) throw bad.apply("Button label is too long (max 40).");
        if (d.actionType() == null) throw bad.apply("Choose what the button does.");
        switch (d.actionType()) {
            case PRODUCT -> { if (d.productId() == null) throw bad.apply("Choose a product."); }
            case OPEN_FOLDER -> { if (d.targetFolderId() == null) throw bad.apply("Choose the folder to open."); }
            case TENDER -> {
                if (d.tenderMethod() == null || !java.util.Set.of("CASH", "CARD", "OTHER").contains(d.tenderMethod())) throw bad.apply("Choose cash, card or other.");
                if (d.amountMinor() != null && d.amountMinor() <= 0) throw bad.apply("A preset amount must be more than zero.");
            }
            case FUNCTION -> { if (d.functionCode() == null || d.functionCode().isBlank()) throw bad.apply("Choose a function."); }
            case SWITCH_MENU -> { if (d.targetMenuId() == null) throw bad.apply("Choose the menu to switch to."); }
        }
        this.folderId = d.folderId();
        this.page = d.page();
        this.gridRow = d.gridRow();
        this.gridCol = d.gridCol();
        this.label = d.label().trim();
        this.color = d.color() == null || d.color().isBlank() ? null : d.color();
        this.actionType = d.actionType();
        this.productId = d.actionType() == ActionType.PRODUCT ? d.productId() : null;
        this.targetFolderId = d.actionType() == ActionType.OPEN_FOLDER ? d.targetFolderId() : null;
        this.tenderMethod = d.actionType() == ActionType.TENDER ? d.tenderMethod() : null;
        this.amountMinor = d.actionType() == ActionType.TENDER ? d.amountMinor() : null;
        this.functionCode = d.actionType() == ActionType.FUNCTION ? d.functionCode() : null;
        this.targetMenuId = d.actionType() == ActionType.SWITCH_MENU ? d.targetMenuId() : null;
        this.active = d.active();
    }

    /** Takes the button out of the grid (frees its slot); synced as active = false. */
    public void remove() {
        this.active = false;
    }

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
