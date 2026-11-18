package com.retailpos.server.sync.contract;

import com.retailpos.server.menu.ActionType;
import com.retailpos.server.menu.MenuButton;
import com.retailpos.server.menu.MenuFolder;
import com.retailpos.server.sync.contract.Contract.MenuButtonV1;
import com.retailpos.server.sync.contract.Contract.MenuFolderV1;

/** Menu entity <-> contract record. Both directions go through the entities' own rules. */
public final class MenuMapper {

    private MenuMapper() {}

    public static MenuFolderV1 toV1(MenuFolder f) {
        return new MenuFolderV1(f.getId(), f.getMenuId(), f.getParentFolderId(), f.getName(), f.isActive());
    }

    public static MenuButtonV1 toV1(MenuButton b) {
        return new MenuButtonV1(b.getId(), b.getFolderId(), b.getPage(), b.getGridRow(), b.getGridCol(), b.getLabel(), b.getColor(),
            b.getActionType().name(), b.getProductId(), b.getTargetFolderId(), b.getTenderMethod(), b.getAmountMinor(),
            b.getFunctionCode(), b.getTargetMenuId(), b.isActive());
    }

    public static MenuButton.Data data(MenuButtonV1 v) {
        return new MenuButton.Data(v.folderId(), v.page(), v.gridRow(), v.gridCol(), v.label(), v.color(),
            ActionType.valueOf(v.actionType()), v.productId(), v.targetFolderId(), v.tenderMethod(), v.amountMinor(),
            v.functionCode(), v.targetMenuId(), v.active());
    }
}
