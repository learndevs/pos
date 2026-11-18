# Decisions and Open Questions

| ID | Status | Question / Decision |
|---|---|---|
| D-001 | Open | Windows 7/8 support required? If yes: Electron 22, Java 11, Spring Boot 2.7 (all EOL). Default: Windows 10+. |
| D-002 | Superseded by D-007 | SQLite (WAL) over H2 for embedded DB. |
| D-003 | Decided | Fully offline; no cloud dependency. |
| D-004 | Decided | Support expiry blocks updates only, never the app. |
| D-005 | Open | Target printer models and connection (USB raw vs Windows spooler). |
| D-007 | Decided | PostgreSQL as the database (chosen with the cloud/multi-terminal roadmap in mind). Cost: bundled Postgres process (+about 100 MB RAM), Windows 10+ only. Revisit only if pilot hardware cannot cope. |
| D-008 | Decided | Scope is single terminal now; multi-terminal hub + cloud dashboard deferred. Schema uses UUIDv7 PKs so that migration stays cheap. |
| D-009 | Decided | Sales model is transaction -> basket items (SALE or TENDER subtype; the basket is the transaction's `basket_item` rows, no separate basket table; totals on the transaction); sale items are MERCHANDISE or NON_MERCHANDISE; modifiers attach to sale items. Refunds are separate REFUND transactions. |
| D-010 | Decided | Server: Spring Boot 4.1, Java 21 bytecode, Gradle, package-by-feature. Dev DB `pos`, test DB `pos_test` (cleaned each test run). |
| D-011 | Decided | The server serves the built UI same-origin (no CORS). Two tokens: per-launch token (Electron to server) and per-user session token from PIN login; sessions in memory. |
| D-012 | Decided | Sale-screen behaviour is an event -> action registry -> effects framework (UI); functions are catalogued in `pos_function`, placed via `menu_button`. ITEM menu opens when a basket line is selected. |
| D-013 | Decided | Backend data model is JPA entities per feature; `PosTransaction` is the aggregate root for basket items (JOINED inheritance). No JdbcTemplate in main code. |
| D-014 | Decided | Menu grid is 4 rows x 3 columns per page (V5); right column split 50/50 keypad / menu. |
| D-015 | Decided | Themes are swappable visual layers over one layout: Default (white), Dark, Liquid Glass. Chosen per terminal; Default recommended on low-end hardware. |
| D-006 | Open | Recipe-level stock deduction in v1 or v1.1? Default: v1.1. |

## Log
- 2026-11-18: Initial spec and plan drafted.
- 2026-11-18: Chose PostgreSQL (D-007); scoped to single terminal (D-008); drafted schema V1/V2.
- 2026-11-18: Reworked sales tables to transaction/basket/basket-item model (D-009), then folded the basket table into pos_transaction; schema behaviour tested on Postgres 17.
- 2026-11-18: Created server project, dev/test databases and schema rule tests (D-010).
- 2026-11-18: Added ui/ and desktop/ projects, PIN sign-in with first-run owner setup (D-011).
- 2026-11-19: Sale screen, action framework + ITEM menu, JPA refactor, 4x3 menu grid (D-012..D-014).
- 2026-11-19: Theme system with Liquid Glass (D-015).
