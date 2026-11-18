import { defineAction } from "./registry";

/** Menu navigation: folders, switching menus, TOTAL, selecting basket lines. */

defineAction({
  id: "OPEN_FOLDER",
  needs: { day: false },
  run: ({ button }) => [{ type: "folder", folderId: button!.targetFolderId! }],
});

defineAction({
  id: "SWITCH_MENU",
  needs: { day: false },
  run: ({ button }) => [{ type: "select" }, { type: "menu", menu: button!.targetMenuCode! }],
});

/** TOTAL: sale menu -> tender menu, and back. */
defineAction({
  id: "TOTAL",
  needs: { items: true },
  run: ({ menu }) => [{ type: "select" }, { type: "menu", menu: menu === "TENDER" ? "SALE" : "TENDER" }],
});

/** Tapping a basket line selects it (ITEM menu opens); tapping it again deselects. */
defineAction({
  id: "SELECT_LINE",
  needs: { sale: true },
  run: ({ lineId, line }) => [{ type: "select", lineId: line?.id === lineId ? undefined : lineId }],
});

defineAction({
  id: "DESELECT_LINE",
  needs: { day: false },
  run: () => [{ type: "select" }],
});
