import type { SessionUser } from "../../../app/authStore";
import type { MenuButton, MenuCode, Sale, SaleLine, saleApi } from "../saleApi";

/**
 * Everything that can happen on the sale screen. All of it goes through one dispatcher (dispatcher.ts).
 *   button  - a configured menu button (product, folder, tender, function, switch menu)
 *   enter   - ENTER key (confirms a prompt, or opens the day)
 *   total   - the big TOTAL button
 *   line    - a basket line was tapped
 */
export type PosEvent =
  | { type: "button"; button: MenuButton }
  | { type: "enter" }
  | { type: "total" }
  | { type: "line"; lineId: string };

/** What an action can see. Actions never touch stores directly; they return Effects. */
export interface ActionContext {
  button?: MenuButton;
  input: number | null;         // keypad value (quantity, or amount in minor units)
  sale: Sale | null;            // the OPEN sale, if any
  line: SaleLine | null;        // the selected basket line, if any
  lineId?: string;              // for "line" events
  menu: MenuCode;
  user: SessionUser;
  dayOpen: boolean;
  api: typeof saleApi;
  money(minor: number): string;
  ensureSale(): Promise<Sale>;  // the open sale, starting one if needed
}

/** Declarative results of an action, applied by the dispatcher in order. */
export type Effect =
  | { type: "sale"; sale: Sale | null }
  | { type: "menu"; menu: MenuCode }
  | { type: "folder"; folderId: string }
  | { type: "select"; lineId?: string }
  | { type: "message"; tone: "info" | "success" | "error"; text: string }
  | { type: "refreshDay" }
  | { type: "signOut" };

export interface ActionDef {
  /** "PRODUCT" | "OPEN_FOLDER" | "TENDER" | "SWITCH_MENU" for button types, a pos_function code, or an internal id. */
  id: string;
  /** Preconditions. Checked before running and used to grey out buttons. */
  needs?: {
    day?: boolean;              // default true: the cash session must be open
    sale?: boolean;             // an OPEN sale must exist
    items?: boolean;            // the sale must have a total > 0
    line?: "SALE" | "ANY";      // a selected basket line of this kind
  };
  /** Ask for a keypad value when none was typed (unless optional). */
  input?: { kind: "number" | "amount"; prompt: (ctx: ActionContext) => string; optional?: boolean };
  /** Ask ENTER = yes / C = no before running. */
  confirm?: (ctx: ActionContext) => string;
  run(ctx: ActionContext): Promise<Effect[]> | Effect[];
}

export const msg = (tone: "info" | "success" | "error", text: string): Effect => ({ type: "message", tone, text });
