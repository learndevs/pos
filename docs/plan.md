# Delivery Plan (about 14-16 weeks, 1-2 devs)

| Phase | Weeks | Deliverable |
|---|---|---|
| 0 Foundations | 1-2 | Repo, Electron launches JVM, signed installer pipeline, bundled PostgreSQL + Flyway, login. Test on a real old Windows PC. |
| 1 Catalog and users | 3-4 | User, role, product, category, modifier CRUD |
| 2 Sales + printing | 5-8 | Transaction/basket screen, tenders, receipt, refunds, drawer kick |
| 3 Cash and day cycle | 9-10 | Day start/end, cash movements, Z-report |
| 4 Inventory | 11-12 | Stock items, auto-deduct, adjustments, alerts |
| 5 Reports + backup | 13-14 | Reports, CSV/PDF, backup/restore |
| 6 Hardening | 15-16 | Licensing, auto-update, crash recovery, pilot, user guide |

## Risks
1. Printer variance: buy 2-3 common models early; keep a printer profile (code page, width).
2. Low-spec PCs: profile memory early; tune JVM.
3. Data safety: prove backup/restore before calling anything done.
4. Spreadsheet migration: CSV import for products and opening stock.

## Status
- [ ] Phase 0
  - [x] Database schema (V1, V2) with rule tests
  - [x] Server project, DB connection, health endpoint
  - [x] PIN login + launch token, sign-in screen
  - [x] Electron launches the server jar (dev + smoke)
  - [ ] Electron starts bundled Postgres
  - [ ] Signed installer pipeline
