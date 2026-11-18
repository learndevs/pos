import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../../api/client";
import { useAuth } from "../../../app/authStore";
import { saleApi, type Sale } from "../saleApi";
import { devices } from "../../../devices/devices";
import { useSaleScreen } from "../saleStore";
import { useSaleData, SALE_KEY } from "../useSaleData";
import { action, actionFor, blockedReason } from "./registry";
import type { ActionContext, ActionDef, Effect, PosEvent } from "./types";

/**
 * The single entry point for sale-screen behaviour:
 *   event -> resolve action -> check guards -> collect input / confirmation -> run -> apply effects
 */
export function usePosDispatch() {
  const qc = useQueryClient();
  const data = useSaleData();
  const ui = useSaleScreen();
  const { user, signOut } = useAuth();

  const openSale = (): Sale | null => (data.sale?.status === "OPEN" ? data.sale : null);

  function context(extra: Partial<ActionContext> = {}): ActionContext {
    const sale = openSale();
    return {
      input: ui.keypad === "" ? null : Number(ui.keypad),
      sale,
      line: sale?.lines.find((l) => l.id === ui.selectedLineId) ?? null,
      menu: ui.menu,
      user: user!,
      dayOpen: !!data.session,
      api: saleApi,
      money: data.money,
      ensureSale: async () => {
        const open = openSale();
        if (open) return open;
        const s = await saleApi.start();
        qc.setQueryData(SALE_KEY, s);
        return s;
      },
      ...extra,
    };
  }

  async function apply(effects: Effect[]) {
    for (const e of effects) {
      switch (e.type) {
        case "sale": qc.setQueryData(SALE_KEY, e.sale); break;
        case "menu": ui.setMenu(e.menu); break;
        case "folder": ui.openFolder(e.folderId); break;
        case "select": ui.select(e.lineId); break;
        case "message": ui.say({ tone: e.tone, text: e.text }); break;
        case "refreshDay": await qc.invalidateQueries({ queryKey: ["cash-session"] }); break;
        case "signOut": signOut(); break;
        case "finishSale": await finishSale(e.sale); break;
      }
    }
  }

  /**
   * End of a sale: open the drawer (cash sales) and wait until it is closed, then print the receipt
   * generated from the transaction, then start the next empty transaction.
   */
  async function finishSale(sale: Sale) {
    ui.setMenu("SALE");
    try {
      if (sale.cashTendered) {
        ui.setMode({ kind: "BUSY", prompt: "Cash drawer is open. Close the drawer to continue." });
        await devices.openDrawer({
          changeText: sale.changeMinor > 0 ? data.money(sale.changeMinor) : undefined,
          totalText: data.money(sale.totalMinor),
        });
      }
      ui.setMode({ kind: "BUSY", prompt: `Printing receipt ${sale.receiptNo}...` });
      let printed = true;
      try {
        await devices.printReceipt(await saleApi.receiptHtml(sale.id), `receipt-${sale.receiptNo}`);
      } catch {
        printed = false;
      }
      const next = await saleApi.start();
      qc.setQueryData(SALE_KEY, next);
      ui.say(
        printed
          ? { tone: "success", text: `Receipt ${sale.receiptNo} printed${sale.changeMinor > 0 ? ` · Change ${data.money(sale.changeMinor)}` : ""}` }
          : { tone: "error", text: `Sale ${sale.receiptNo} completed, but the receipt did not print.` },
      );
    } finally {
      ui.setMode({ kind: "IDLE" });
    }
  }

  async function run(def: ActionDef, ctx: ActionContext) {
    try {
      ui.say(undefined);
      await apply(await def.run(ctx));
    } catch (e) {
      ui.say({ tone: "error", text: e instanceof ApiError || e instanceof Error ? e.message : "Something went wrong." });
    }
  }

  /** Guard + input + confirm, then run. */
  async function start(def: ActionDef | undefined, extra: Partial<ActionContext>) {
    if (ui.mode.kind === "BUSY") return; // finishing a sale: ignore presses
    const ctx = context(extra);
    const blocked = blockedReason(def, ctx, extra.button);
    if (blocked || !def) return ui.say({ tone: "error", text: blocked ?? "Not available." });
    ui.clearKeypad();
    if (def.input && ctx.input == null && !def.input.optional) {
      return ui.setMode({ kind: "INPUT", def, ctx, prompt: def.input.prompt(ctx) });
    }
    if (def.confirm) return ui.setMode({ kind: "CONFIRM", def, ctx, prompt: def.confirm(ctx) });
    ui.setMode({ kind: "IDLE" });
    await run(def, ctx);
  }

  async function dispatch(event: PosEvent) {
    switch (event.type) {
      case "button":
        return start(actionFor(event.button), { button: event.button });
      case "total":
        return start(action("TOTAL"), {});
      case "line":
        return start(action("SELECT_LINE"), { lineId: event.lineId });
      case "product":
        return start(action("PRODUCT"), { productId: event.productId });
      case "enter": {
        const mode = ui.mode;
        if (mode.kind === "BUSY") return;
        if (mode.kind === "INPUT") {
          if (ui.keypad === "") return ui.say({ tone: "error", text: "Type a number first." });
          const input = Number(ui.keypad);
          ui.clearKeypad();
          ui.setMode({ kind: "IDLE" });
          return run(mode.def, { ...mode.ctx, input });
        }
        if (mode.kind === "CONFIRM") {
          ui.setMode({ kind: "IDLE" });
          return run(mode.def, mode.ctx);
        }
        if (!data.session) {
          const ctx = context();
          ui.clearKeypad();
          return run(action("OPEN_DAY"), ctx);
        }
        return;
      }
    }
  }

  /**
   * For greying out buttons: the same guards as a real press, except "day not open"
   * (buttons stay pressable so the press can explain how to start the day).
   */
  const isEnabled = (button: Parameters<typeof actionFor>[0]) =>
    !blockedReason(actionFor(button), { ...context({ button }), dayOpen: true }, button);

  function cancelPrompt() {
    if (ui.mode.kind === "BUSY") return;
    ui.setMode({ kind: "IDLE" });
    ui.clearKeypad();
    ui.say(undefined);
  }

  return { dispatch, isEnabled, cancelPrompt };
}
