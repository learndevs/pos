import { useMemo } from "react";
import type { MenuButton } from "./saleApi";
import { useSaleScreen } from "./saleStore";
import { usePosDispatch } from "./actions";
import { useSaleData } from "./useSaleData";

const ROWS = 4;

/** Clean palette: style follows what the button does, not per-button colours. */
function styleOf(b: MenuButton): string {
  if (b.actionType === "OPEN_FOLDER") return "folder";
  if (b.actionType === "TENDER") return "tender";
  if (b.actionType === "PRODUCT") return "product";
  if (b.functionCode === "CANCEL_TXN" || b.functionCode === "VOID_LINE") return "fn danger";
  if (b.functionCode === "DESELECT_LINE") return "fn done";
  return "fn";
}
const COLS = 3;

/** 4 x 3 grid of configured buttons for the current folder and page, with a nav strip above it. */
export function MenuBar() {
  const { menus, money } = useSaleData();
  const ui = useSaleScreen();
  const { dispatch, isEnabled } = usePosDispatch();
  const selectedLine = useSaleData().sale?.lines.find((l) => l.id === ui.selectedLineId);

  const menu = menus?.menus.find((m) => m.code === ui.menu);
  const folderId = ui.folderStack.at(-1) ?? menu?.rootFolderId;
  const folderButtons = useMemo(() => menus?.buttons.filter((b) => b.folderId === folderId) ?? [], [menus, folderId]);
  const pages = Math.max(1, ...folderButtons.map((b) => b.page + 1));
  const page = Math.min(ui.page, pages - 1);

  const grid: (MenuButton | undefined)[] = Array.from({ length: ROWS * COLS }, () => undefined);
  for (const b of folderButtons) if (b.page === page) grid[b.gridRow * COLS + b.gridCol] = b;

  const crumbs = [
    ui.menu === "ITEM" && selectedLine ? `Line ${selectedLine.lineNo} · ${selectedLine.name ?? selectedLine.tenderMethod}` : menu?.name ?? "",
    ...ui.folderStack.map((id) => menus?.folders.find((f) => f.id === id)?.name ?? ""),
  ];

  return (
    <section className="menu-bar">
      <nav className="menu-nav">
        <span className="crumbs">{crumbs.join(" › ")}</span>
        <span className="spacer" />
        {ui.folderStack.length > 0 && (
          <>
            <button type="button" className="nav-btn" onClick={ui.back} title="Back">‹ Back</button>
            <button type="button" className="nav-btn" onClick={ui.home} title="Home">Home</button>
          </>
        )}
        {pages > 1 && (
          <span className="pager">
            <button type="button" className="nav-btn icon" onClick={() => ui.setPage(page - 1)} disabled={page === 0} aria-label="Previous page">‹</button>
            <span className="page">{page + 1} / {pages}</span>
            <button type="button" className="nav-btn icon" onClick={() => ui.setPage(page + 1)} disabled={page >= pages - 1} aria-label="Next page">›</button>
          </span>
        )}
      </nav>
      <div className="menu-grid">
        {grid.map((b, i) =>
          b ? (
            <button
              key={b.id}
              type="button"
              className={`menu-btn ${styleOf(b)}`}
              disabled={!isEnabled(b)}
              onClick={() => void dispatch({ type: "button", button: b })}
            >
              <span className="menu-label">{b.label}</span>
              {b.actionType === "OPEN_FOLDER" && <span className="menu-hint">›</span>}
              {b.actionType === "PRODUCT" && b.productPriceMinor != null && <span className="menu-hint">{money(b.productPriceMinor)}</span>}
            </button>
          ) : (
            <div key={`empty-${i}`} className="menu-btn empty" />
          ),
        )}
      </div>
    </section>
  );
}
