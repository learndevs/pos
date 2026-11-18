---
name: pos-printing
description: Receipts (HTML Mustache template rendered from the transaction), test printer / test cash drawer windows, and later ESC/POS thermal printing. Use when changing receipt layout, printing, or drawer behaviour.
---

# Printing

## Now: HTML receipts + test devices
- Receipt = active `receipt_template` (Mustache HTML) rendered on the server from a COMPLETED transaction (`receipt/ReceiptService`). Placeholders and API: `docs/receipts.md`. Never build receipt text in the UI.
- Desktop devices (`desktop/src/devices/`), reached from the UI only via the preload bridge (`window.pos.openDrawer`, `window.pos.printReceipt`) through `ui/src/devices/devices.ts`:
  - `testDrawer.ts`: "Cash Drawer (test)" window; the promise resolves when it is closed (= drawer closed sensor).
  - `testPrinter.ts`: renders HTML offscreen to an 80mm PDF cut at the content height, saves to `<userData>/receipts/`, shows it in the "Test Printer" window.
  - Plain browser (no bridge): in-page drawer simulator and a new tab with the receipt HTML.
- Sale end order (dispatcher `finishSale`): drawer (cash sales only) -> wait for close -> receipt -> print -> next transaction. A print failure never undoes the sale; it shows an error and the sale stays completed.

## Later: real hardware

- Server builds ESC/POS bytes (`escpos-coffee`); transport is either Windows spooler in raw mode or direct USB via Electron IPC fallback. Transport is selected in settings (see `docs/decisions.md` D-005).
- **Printer profile** (settings): paper width (48 cols at 80 mm), code page, cut command, drawer pin. Never hardcode.
- Templates: receipt, reprint (marked "DUPLICATE"), kitchen ticket, Z-report, cash-in/out slip. Keep layout in one template module so wording and columns are testable.
- Receipts print after the sale transaction is COMPLETED and committed; a print failure must never roll back a sale. Surface "Reprint" and a retry queue.
- Drawer kick (ESC p) fires on cash tender and manual "open drawer" (permission-gated and audit-logged).
- Test by rendering bytes to a text preview in unit tests; verify on at least two physical printer models before release.
- Handle: out of paper, cover open, offline printer, with clear UI messages.
