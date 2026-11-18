import type { ActionContext, ActionDef } from "./types";
import type { MenuButton } from "../saleApi";

const actions = new Map<string, ActionDef>();

/** Register an action. One call per behaviour; ids must be unique. */
export function defineAction(def: ActionDef): ActionDef {
  // Hot reload re-runs action modules in dev, so only a production duplicate is an error.
  if (actions.has(def.id) && !import.meta.env.DEV) throw new Error(`Duplicate POS action: ${def.id}`);
  actions.set(def.id, def);
  return def;
}

export function actionFor(button: MenuButton): ActionDef | undefined {
  return actions.get(button.actionType === "FUNCTION" ? button.functionCode ?? "" : button.actionType);
}

export function action(id: string): ActionDef {
  const a = actions.get(id);
  if (!a) throw new Error(`Unknown POS action: ${id}`);
  return a;
}

export function allActions(): ActionDef[] {
  return [...actions.values()];
}

/**
 * Why an action cannot run right now (or undefined if it can). Used both to block a press
 * and to grey out its button. Button metadata (permission, availability) comes from the DB.
 */
export function blockedReason(def: ActionDef | undefined, ctx: ActionContext, button?: MenuButton): string | undefined {
  if (!def || button?.functionAvailable === false) return `${button?.label ?? "This"} is not available yet.`;
  if (button?.requiredPermission && !ctx.user.permissions.includes(button.requiredPermission)) {
    return `You are not allowed to use ${button.label}.`;
  }
  const n = def.needs ?? {};
  if ((n.day ?? true) && !ctx.dayOpen) return "Start the day first: enter the opening float and press ENTER.";
  if (n.sale && !ctx.sale) return "No sale in progress.";
  if (n.items && (!ctx.sale || ctx.sale.totalMinor === 0)) return "Add items first.";
  if (n.line && !ctx.line) return "Select a basket line first.";
  if (n.line === "SALE" && ctx.line?.kind !== "SALE") return "Select an item line (not a payment).";
  return undefined;
}
