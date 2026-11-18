# Tech Spec

## 1. Target
Small restaurant, single outlet, one terminal, one 80 mm ESC/POS thermal printer, legacy Windows. Fully offline.

## 2. Platform assumptions
- Windows 10 64-bit minimum (Win 7/8 only if a pilot customer requires it, see decisions.md D-001).
- 4 GB RAM, dual core, 10 GB free disk.
- Electron + JVM idle around 500-700 MB.

## 3. Architecture
```
Electron main ── spawns bundled JRE ── Spring Boot (127.0.0.1:random port)
Electron renderer (React) ── HTTP + per-launch bearer token ── REST API
Spring Boot ── JPA ── PostgreSQL (bundled, data in %APPDATA%/<App>/pgdata, 127.0.0.1 only), Flyway, nightly pg_dump
Electron IPC ── printer fallback, cash drawer, file dialogs
```
- Main waits for `/actuator/health` before showing the window; shows splash meanwhile (JVM start 5-15 s).
- Main kills the JVM on quit and restarts it on crash.
- Renderer: `contextIsolation` on, `nodeIntegration` off, preload bridge only.

## 4. Stack
| Layer | Choice |
|---|---|
| Shell | Electron (stable), electron-builder + NSIS, auto-update |
| UI | React, TypeScript, Vite, Tailwind or Mantine, TanStack Query, Zustand |
| Backend | Spring Boot 3.x, Java 17/21, Web, Data JPA, Security (PIN login), Validation |
| DB | PostgreSQL 17 (bundled binaries, child process) + Flyway. See [database-schema.md](database-schema.md) |
| Runtime | `jlink` minimal JRE (about 40-60 MB) bundled |
| Reports | Server aggregates, CSV, PDF via OpenPDF |
| Printing | `escpos-coffee` |
| Licensing | Offline Ed25519-signed license file bound to machine ID |
| Tests | JUnit 5, Vitest, Playwright (Electron E2E) |
| CI | GitHub Actions, Windows runner, signed installer |

## 5. Domain model
Sales: `pos_transaction` -> `basket_item` (`sale_item` merchandise/non-merchandise, `tender_item`) with `sale_item_modifier`. Plus `user`, `role`, `category`, `product`, `modifier*`, `dining_table`, `cash_session`, `cash_movement`, `stock_item`, `recipe_item`, `stock_movement`, `print_job`, `audit_log`, `app_setting`, `receipt_counter`. Full detail: [database-schema.md](database-schema.md).

Rules:
- Money in integer minor units.
- Completed transactions are immutable; corrections are REFUND transactions. Stock and cash movements are append-only.
- Every sale has `business_date` and `cash_session_id`.

## 6. Functional scope (v1)
Users and roles (Owner/Manager/Cashier, PIN) · Products and modifiers · Inventory (product-level, auto-deduct, alerts) · Day start (opening float) · Sales (table/takeaway, discount, tender, change) · Receipt print/reprint (+ optional kitchen ticket) · Cash movements and drawer kick · Day end (expected vs counted, Z-report, lock) · Reports with CSV export · Daily backup and restore · CSV import for products/opening stock.

Out of scope: multi-outlet, cloud sync, online ordering, loyalty, card terminal integration (card is a tender type only).

## 7. Licensing and revenue
- One-time fee: license file per machine, no recurring check-in.
- Annual support: `support_until` in the license. After expiry the app keeps working; updates and support stop.

## 8. Non-functional targets
- Cold start < 20 s on 4 GB dual core.
- Add item to order < 100 ms.
- No data loss on power cut (Postgres WAL, default `synchronous_commit=on`).
- Touch and keyboard usable; receipts always reprintable.
- JVM tuning start point: `-Xmx256m -XX:+UseSerialGC`.
