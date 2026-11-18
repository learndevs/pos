---
name: pos-domain
description: Domain rules for the POS: money, sales, stock ledgers, cash sessions, business date, refunds. Use when touching any code or schema involving transactions, baskets, tenders, inventory, or day start/end.
---

# POS domain rules

1. **Money** is `long` minor units (cents, `bigint`). Never `float`/`double` in storage or API. Format only in the UI. Rounding happens once, at line-tax calculation; document the mode (half-up).
2. **Sales model:** `pos_transaction` (holds totals) -> `basket_item` (kind SALE or TENDER) -> subtype `sale_item` / `tender_item`; `sale_item_modifier` hangs off sale items. Sale items are MERCHANDISE (product, stock-tracked) or NON_MERCHANDISE (service charge, delivery, tip, deposit, gift card, other; no stock).
3. **Immutability.** A transaction is editable only while OPEN; COMPLETED/CANCELLED is final (DB enforced). Corrections are a new REFUND transaction linked to the original (`original_transaction_id`, reason required), never an edit. Amounts are stored positive; use `v_sale_item_signed`/`v_tender_signed` for net figures. `stock_movement`, `cash_movement`, `audit_log` are append-only.
4. **Business date vs timestamp.** Every sale stores `business_date` (the open cash session's date) separate from `created_at`. Late-night sales after midnight belong to the open session.
5. **Cash session.** No transaction without an open session; day close is refused while any transaction is OPEN. Day end computes expected cash (float + net cash tenders + paid-in - paid-out - expenses), records counted cash and variance, produces a Z-report, and locks that date.
6. **Stock.** Quantity on hand = sum of `stock_movement`. Only MERCHANDISE deducts stock, in the same DB transaction as completion. Adjustments need a reason code.
7. **Receipt numbers** come from `receipt_counter`, gap-free per business date, allocated only when a transaction is COMPLETED (cancelled ones consume none).
8. **Roles:** Cashier sells; Manager voids, discounts over limit, adjusts stock; Owner manages users, settings, license.
9. **Power-cut safety:** completion, receipt number and stock deduction commit in one transaction; a sale is either fully committed or absent.
10. **DB enforces:** triggers freeze finished transactions and keep the ledgers append-only; no writes against a CLOSED session. See `docs/database-schema.md`.

When changing any of these, update `docs/tech-spec.md` section 5 and log it in `docs/decisions.md`.
11. **Menus:** `SALE`, `TENDER`, `ITEM` (line selected). Each is a folder tree; each page is a 4 x 3 button grid. Button types: PRODUCT, OPEN_FOLDER, TENDER, FUNCTION (a `pos_function` code), SWITCH_MENU.
