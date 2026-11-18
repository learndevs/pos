---
name: pos-printing
description: Receipt, kitchen ticket, Z-report and cash drawer printing via ESC/POS on an 80 mm thermal printer. Use when changing receipt layout, printer setup, or drawer behavior.
---

# Printing

- Server builds ESC/POS bytes (`escpos-coffee`); transport is either Windows spooler in raw mode or direct USB via Electron IPC fallback. Transport is selected in settings (see `docs/decisions.md` D-005).
- **Printer profile** (settings): paper width (48 cols at 80 mm), code page, cut command, drawer pin. Never hardcode.
- Templates: receipt, reprint (marked "DUPLICATE"), kitchen ticket, Z-report, cash-in/out slip. Keep layout in one template module so wording and columns are testable.
- Receipts print after the sale transaction is COMPLETED and committed; a print failure must never roll back a sale. Surface "Reprint" and a retry queue.
- Drawer kick (ESC p) fires on cash tender and manual "open drawer" (permission-gated and audit-logged).
- Test by rendering bytes to a text preview in unit tests; verify on at least two physical printer models before release.
- Handle: out of paper, cover open, offline printer, with clear UI messages.
