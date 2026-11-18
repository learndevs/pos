---
name: pos-sale-actions
description: How sale-screen behaviour works (event -> action registry -> effects) and the checklist for adding or changing a button, function, menu or keypad behaviour. Use when touching ui/src/features/sales, menu_button / pos_function data, or anything a till button does.
---

# Sale screen actions framework

Visual reference (diagrams, action table): `docs/sale-screen.md`.

## How a press flows
```
PosEvent (button | enter | total | line)
  -> dispatcher (actions/dispatcher.ts)
     -> resolve ActionDef (registry: button.actionType, or pos_function code for FUNCTION buttons)
     -> guards: blockedReason()  (day open, sale, items, selected line, permission, available)
     -> input?   no keypad value -> mode INPUT  (dialog prompt, ENTER runs it)
     -> confirm? -> mode CONFIRM (ENTER = yes, Clear = no)
     -> run(ctx) -> Effect[]
     -> apply effects in order: sale | menu | folder | select | message | refreshDay | signOut
```
- The keypad value is the argument for the next press: quantity before a product, amount (minor units) before a tender, number for an INPUT prompt.
- Menus: `SALE` (default), `TENDER` (TOTAL toggles), `ITEM` (opens when a basket line is selected; Done / deselect returns to the previous menu).
- Buttons grey out from the same guards (`isEnabled`), except "day not open", which stays pressable to explain itself.

## Files
`ui/src/features/sales/actions/`
- `types.ts` PosEvent, ActionContext, Effect, ActionDef
- `registry.ts` defineAction, actionFor, blockedReason
- `dispatcher.ts` usePosDispatch (the only place that mutates screen state)
- `navigation.ts` OPEN_FOLDER, SWITCH_MENU, TOTAL, SELECT_LINE, DESELECT_LINE
- `sale.ts` PRODUCT, TAKEAWAY_TOGGLE, CANCEL_TXN, OPEN_DAY
- `line.ts` QTY, QTY_PLUS, QTY_MINUS, VOID_LINE (ITEM menu)
- `tender.ts` TENDER
- `system.ts` OPEN_DRAWER, SIGN_OUT
- `index.ts` imports every action file (register new files here)

## Rules
1. Actions are pure-ish: read `ctx`, call `ctx.api`, return `Effect[]`. Never import stores or call `set...` inside an action.
2. Declare preconditions in `needs` / `input` / `confirm`; do not hand-check them in `run`.
3. Business rules (totals, tax, what can be tendered) live on the server (`PosTransaction`). The UI only shows results.
4. Error messages are for the cashier: short, plain, actionable.
5. Permissions come from `pos_function.required_permission`; the server must also enforce them (`@PreAuthorize`).

## Checklist: add a new function button (e.g. LINE_DISCOUNT)
1. **DB** new migration: `INSERT INTO pos_function (code, name, input, required_permission, needs_selected_line, available)`; set `available = true` when implemented (existing row: `UPDATE`).
2. **Place it** in a menu (same migration): `INSERT INTO menu_button (... folder_id, page, grid_row 0-3, grid_col 0-2, label, action_type 'FUNCTION', function_code)`. ITEM root folder = `...0b0003`, SALE root = `...0b0001`, TENDER root = `...0b0002`.
3. **Server** (if it changes data): domain method on the aggregate (`PosTransaction`), service method, controller endpoint with `@PreAuthorize`; add to `saleApi.ts`.
4. **UI action**: `defineAction({ id: "LINE_DISCOUNT", needs: {...}, input: {...}, run })` in the right file under `actions/`.
5. **Docs**: add a row to the action table in `docs/sale-screen.md`.
6. Verify by running the app (keep tests light: a server flow test only if money/stock logic changed).

## Add a new button type
Add the value to `menu_button.action_type` CHECK + a column if needed (migration), `ActionType` enum (server), `ActionType` union (`saleApi.ts`), then `defineAction({ id: "<TYPE>" })`.
