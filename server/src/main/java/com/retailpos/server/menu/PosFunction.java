package com.retailpos.server.menu;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catalogue entry for a built-in function a button may call. Behaviour lives in the UI action registry. */
@Entity
@Table(name = "pos_function")
public class PosFunction {

    @Id
    private String code;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String input;              // NONE | NUMBER | AMOUNT | CONFIRM
    @Column(name = "required_permission") private String requiredPermission;
    @Column(name = "needs_selected_line", nullable = false) private boolean needsSelectedLine;
    @Column(nullable = false) private boolean available;

    protected PosFunction() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getInput() { return input; }
    public String getRequiredPermission() { return requiredPermission; }
    public boolean isNeedsSelectedLine() { return needsSelectedLine; }
    public boolean isAvailable() { return available; }
}
