# Basic Local Windows POS

Offline point-of-sale for a single-outlet restaurant on a legacy Windows PC (1 terminal + 1 receipt printer). Replaces spreadsheets.

**Stack:** Electron + React (UI) · Spring Boot (local API) · PostgreSQL · ESC/POS thermal printing.
**Revenue:** one-time software license + annual support.

## Docs
- [docs/architecture.md](docs/architecture.md): **start here**: system map, request flow, sale lifecycle, where code goes
- [docs/features/](docs/features/): one spec per feature (+ template)
- [docs/features/admin-sync.md](docs/features/admin-sync.md): admin panel, config packages, transaction archive
- [docs/plans/server-modules.md](docs/plans/server-modules.md): plan to split the server into Gradle modules
- [docs/tech-spec.md](docs/tech-spec.md): architecture, stack, data model, scope, NFRs
- [docs/database-schema.md](docs/database-schema.md): schema, ERD, integrity rules
- [docs/sale-screen.md](docs/sale-screen.md): sale screen layout, menus, action framework
- [docs/receipts.md](docs/receipts.md): receipt template placeholders and API
- [docs/plan.md](docs/plan.md): phased delivery plan and risks
- [docs/dev-setup.md](docs/dev-setup.md): local DB and server setup
- [docs/decisions.md](docs/decisions.md): open questions and decision log (ADR-lite)

## Project skills (`.claude/skills/`)
| Skill | Use when |
|---|---|
| `pos-feature` | **Any new feature**: end-to-end workflow, code skeletons, definition of done |
| `pos-admin` | Admin panel screens and APIs: feature registry, inspectors/tabs, view/edit/delete, broadcastable entities |
| `pos-domain` | Touching money, sales, stock, cash sessions, business date |
| `pos-backend` | Working in `server/`: layout, DB connection, Flyway, run and test |
| `pos-desktop` | Working in `desktop/` or `ui/` (Electron, JVM launcher, security) |
| `pos-printing` | Receipt template, test printer / drawer windows, ESC/POS later |
| `pos-sale-actions` | Sale screen buttons, functions, menus, keypad behaviour |
| `pos-docs` | Updating the docs or decision log |

## Run
```bash
npx npm@11 install
npm run dev          # POS: server + UI + Electron window
npm run dev:admin    # admin panel: admin-app :8081 + admin-ui http://localhost:5175
```
Setup details: [docs/dev-setup.md](docs/dev-setup.md).

## Layout
```
tools/e2e/  drive the real Electron app against a throwaway test stack (screenshots)
```
```
desktop/  Electron main, preload, electron-builder config
ui/       React + TypeScript + Vite (sign-in, sale screen)
server/   Spring Boot 4 + Flyway (Gradle) - see server/README.md
docs/
```
