# Basic Local Windows POS

Offline point-of-sale for a single-outlet restaurant on a legacy Windows PC (1 terminal + 1 receipt printer). Replaces spreadsheets.

**Stack:** Electron + React (UI) · Spring Boot (local API) · PostgreSQL · ESC/POS thermal printing.
**Revenue:** one-time software license + annual support.

## Docs
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
| `pos-domain` | Touching money, sales, stock, cash sessions, business date |
| `pos-backend` | Working in `server/`: layout, DB connection, Flyway, run and test |
| `pos-desktop` | Working in `desktop/` or `ui/` (Electron, JVM launcher, security) |
| `pos-printing` | Receipt template, test printer / drawer windows, ESC/POS later |
| `pos-sale-actions` | Sale screen buttons, functions, menus, keypad behaviour |
| `pos-docs` | Updating the docs or decision log |

## Run
```bash
npx npm@11 install
npm run dev        # server + UI + Electron window
```
Setup details: [docs/dev-setup.md](docs/dev-setup.md).

## Layout
```
desktop/  Electron main, preload, electron-builder config
ui/       React + TypeScript + Vite (sign-in, sale screen)
server/   Spring Boot 4 + Flyway (Gradle) - see server/README.md
docs/
```
