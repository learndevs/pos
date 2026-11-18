import { create } from "zustand";
import type { MenuCode } from "./saleApi";
import type { ActionContext, ActionDef } from "./actions/types";

/** What the dialog panel is waiting for. */
export type Mode =
  | { kind: "IDLE" }
  | { kind: "INPUT"; def: ActionDef; ctx: ActionContext; prompt: string }    // e.g. Set qty with no number typed
  | { kind: "CONFIRM"; def: ActionDef; ctx: ActionContext; prompt: string }; // e.g. "Void line 2?"

export type Message = { tone: "info" | "success" | "error"; text: string } | undefined;

interface SaleScreenState {
  keypad: string;
  menu: MenuCode;
  /** Menu to return to when the ITEM menu closes (line deselected). */
  returnMenu: MenuCode;
  folderStack: string[];
  page: number;
  selectedLineId?: string;
  mode: Mode;
  message: Message;
  press(key: string): void;
  backspace(): void;
  clearKeypad(): void;
  setMenu(menu: MenuCode): void;
  openFolder(id: string): void;
  back(): void;
  home(): void;
  setPage(p: number): void;
  /** Selecting a line opens the ITEM menu; deselecting returns to the previous menu. */
  select(lineId?: string): void;
  setMode(mode: Mode): void;
  say(message: Message): void;
}

export const useSaleScreen = create<SaleScreenState>((set) => ({
  keypad: "",
  menu: "SALE",
  returnMenu: "SALE",
  folderStack: [],
  page: 0,
  mode: { kind: "IDLE" },
  message: undefined,
  press: (key) => set((s) => ({ keypad: (s.keypad + key).replace(/^0+(?=\d)/, "").slice(0, 9), message: undefined })),
  backspace: () => set((s) => ({ keypad: s.keypad.slice(0, -1) })),
  clearKeypad: () => set({ keypad: "" }),
  setMenu: (menu) => set((s) => ({ menu, folderStack: [], page: 0, returnMenu: menu === "ITEM" ? s.returnMenu : menu })),
  openFolder: (id) => set((s) => ({ folderStack: [...s.folderStack, id], page: 0 })),
  back: () => set((s) => ({ folderStack: s.folderStack.slice(0, -1), page: 0 })),
  home: () => set({ folderStack: [], page: 0 }),
  setPage: (page) => set({ page }),
  select: (selectedLineId) =>
    set((s) =>
      selectedLineId
        ? { selectedLineId, menu: "ITEM", folderStack: [], page: 0, returnMenu: s.menu === "ITEM" ? s.returnMenu : s.menu }
        : { selectedLineId: undefined, menu: s.menu === "ITEM" ? s.returnMenu : s.menu, folderStack: s.menu === "ITEM" ? [] : s.folderStack },
    ),
  setMode: (mode) => set({ mode }),
  say: (message) => set({ message }),
}));
