# Plan: Admin panel + configuration sync

Status: **phase 1 done** (2026-11-19): catalog packages, till apply + ack, transaction archive, admin UI. Admin runs **locally for now**, built cloud-ready.

## Decisions (updated 2026-11-19 with the product owner)
- Separate admin web app + DB (`pos_admin`); POS keeps `pos`. Shared jars (`catalog`, `sync-contract`) = one definition.
- **Configuration is one-way** (admin -> POS). Admin is the only writer of master data.
- **Packages**: the admin chooses **entities** (tax rates, categories, products; more later), a **scope** (changes made
  between two **dates**, inclusive, or a full snapshot) and **target tills** (all ticked by default), then
  **Create & broadcast**. The package is a **zip**: `manifest.json` (package no., contract version, scope, counts,
  SHA-256 per file) + one JSON file per entity, each item the latest full record. Referenced records (a product's
  category / tax rate) are added automatically. Re-applying a package is harmless.
- "Broadcast" = the package becomes available to its targets; tills fetch it (they open the connection; routers
  block inbound). Check interval: 60 s by default (`pos.sync.interval-ms`). Instant notification (SSE) later.
- The till verifies checksums + contract version, applies a package in **one DB transaction**, and reports
  APPLIED / FAILED (with reason) back; the admin shows status per till. Failed packages are retried each check.
- Gap warning: a date-range package warns if a target till has not applied anything since before the range start.
- Offline shops: a package zip can be downloaded from admin and imported on the till (`POST /api/sync/import`).
- **Transactions are archived** (POS -> admin): every finished transaction (completed **and cancelled**) is written
  to a POS outbox in the same DB transaction, uploaded in batches (retried until accepted, idempotent by id) and
  stored in `transaction_archive` (full JSON); the admin has a Transactions screen.

## Migrations: one global version sequence, folder = which DB runs it
| Folder | Runs on | Example |
|---|---|---|
| `server/app/.../db/migration` | POS | V1-V7, V10 `sync_state`, V12 packages applied + transaction outbox |
| `server/modules/master-schema/.../db/master` | POS **and** admin | V8 master baseline (`CREATE TABLE IF NOT EXISTS` = no-op on existing POS DBs) |
| `server/admin-app/.../db/admin` | admin | V9 change log, terminals, admin users; V11 packages, targets, transaction archive |
Pick the next free number across all folders. A shared-table change goes in `db/master`.

## Modules
```
modules/master-schema   shared master-data migrations (db/master)
modules/sync-contract   ConfigChange, ChangesPage, TaxRateV1, CategoryV1, ProductV1 + entity -> V1 mappers
modules/sync-server     admin: change log, publish, terminals, feed API /sync/v1/changes
modules/sync-client     POS: poll, apply in one transaction, cursor, status API
admin-app               Spring Boot (admin): admin auth, catalog edit API, serves admin-ui -> admin.jar
admin-ui                React admin panel (Vite :5175 in dev)
```

## Phases
| Phase | Scope |
|---|---|
| **1** | Packages (zip) for tax rates, categories, products; POS apply + ack; transaction archive; admin UI: catalog, packages, terminals, transactions |
| 2 | Menus + button grid editor, settings, receipt template editor with preview, till users |
| 3 | Push notification, sync status details, terminal management UI polish |
| 4 | Reverse sync: POS sales outbox -> admin reports (cloud dashboard) |

## Local run
`npm run dev` (POS) and `npm run dev:admin` (admin-app :8081 + admin-ui :5175). Dev terminal token: `dev-terminal-token` (dev only).
