# Receipts

The receipt is HTML generated from a completed transaction with a **Mustache template** stored in
`receipt_template` (one active row). Change the layout without code changes.

## Flow
```
TENDER completes the sale ─▶ drawer opens (cash sales) ─▶ wait for "drawer closed"
  ─▶ GET /api/receipts/{txnId}  (server renders the active template with the sale)
  ─▶ desktop prints it (test printer: 80mm PDF in the "Test Printer" window, saved to <userData>/receipts)
  ─▶ next empty transaction starts
```

## API
| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/api/receipts/{txnId}` | ORDER_CREATE | Rendered HTML for a COMPLETED sale |
| GET | `/api/settings/receipt-template` | SETTINGS_MANAGE | Active template `{id, name, html, paperWidthMm}` |
| PUT | `/api/settings/receipt-template` | SETTINGS_MANAGE | Save `{name, html, paperWidthMm}`; rejected if it does not compile |
| POST | `/api/settings/receipt-template/preview` | SETTINGS_MANAGE | Render `{html, transactionId?}` (sample data when no id) without saving |

## Template data
Values are HTML-escaped. Empty strings count as "missing", so `{{#business.phone}}...{{/business.phone}}` hides the line when there is no phone.

| Placeholder | Example |
|---|---|
| `{{business.name}}`, `{{business.address}}`, `{{business.phone}}` | from `app_setting` `business` |
| `{{receipt.header}}`, `{{receipt.footer}}` | from `app_setting` `receipt` |
| `{{sale.receiptNo}}`, `{{sale.txnNo}}` | `20261119-0001`, `1` |
| `{{sale.date}}`, `{{sale.time}}`, `{{sale.businessDate}}` | `2026-11-19`, `14:32` |
| `{{sale.cashier}}`, `{{sale.orderType}}` | `Amy`, `Takeaway` |
| `{{#lines}} {{qty}} {{name}} {{unitPrice}} {{amount}} {{#showUnit}}..{{/showUnit}} {{/lines}}` | one per sale item (`showUnit` when qty is not 1) |
| `{{#tenders}} {{method}} {{amount}} {{tendered}} {{/tenders}}` | `Cash`, applied amount, amount handed over |
| `{{totals.subtotal}}`, `{{totals.tax}}`, `{{totals.total}}`, `{{totals.paid}}`, `{{totals.change}}` | formatted money |
| `{{#totals.hasChange}}..{{/totals.hasChange}}` | only when change was given |

## Writing a template
- Plain HTML + inline `<style>`; no scripts or external files (printing is offline).
- Design for the paper: `@page { size: 80mm auto; margin: 0 }`, `body { width: 80mm }`, monospace 11-12px.
- The test printer cuts the page right after the content, like a thermal printer.
- Default template: `V6__txn_number_receipt_template.sql`.
