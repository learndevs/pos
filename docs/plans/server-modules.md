# Plan: split the server into Gradle modules by functionality

Status: **done 2026-11-19** (steps 1-6, 8). Step 7 (`api` / `internal` packages + boundary test) is deferred: Gradle module dependencies already enforce the direction; revisit when modules are reused by the cloud service.

## Why
- Clear boundaries: a module can only use what it declares as a dependency, so the compiler stops
  accidental coupling (today it is only a convention).
- Easier for humans and AI developers to work on one feature without reading the whole server.
- Prepares for the cloud/multi-terminal phase (some modules will be reused by a cloud service).

## Target layout
```
server/
  settings.gradle                      include all modules
  build.gradle                         shared config (Java 21, Spring BOM, test setup)
  platform/                            shared kernel: BaseEntity, UuidV7, PosException, ClockConfig
  modules/
    identity/                          users, roles, PIN auth, sessions        -> platform, audit
    audit/                             audit_log                               -> platform
    settings/                          app_setting                             -> platform
    catalog/                           products, categories, tax, search       -> platform
    inventory/                         stock items, recipes, ledger            -> platform, sales (SaleCompleted event)
    cash/                              day start/end, cash movements           -> platform, identity-api
    menu/                              menus, folders, buttons, pos_function   -> platform, catalog
    sales/                             transaction aggregate, tenders          -> platform, catalog, cash, identity, settings, audit
    receipt/                           templates, rendering                    -> platform, sales, settings, identity
  app/                                 Spring Boot main, SecurityConfig, filters, application.yml,
                                       ALL Flyway migrations, bootJar (server.jar), integration tests
```
Package names stay `com.retailpos.server.<feature>`, so the move is mostly files, not code.

## Module rules
1. Each module has `api` (public services, view records, events) and `internal` (entities, repositories) packages; other modules use `api` only.
2. No cycles. Dependency direction as in [architecture.md](../architecture.md) section 5.
3. **Events instead of upward calls**: `sales` publishes `SaleCompleted` (in-transaction `@EventListener`); `inventory` listens and deducts stock. Removes `sales -> inventory`.
4. **Migrations stay in `app`** (one Flyway history, one version sequence). Each migration file header names its module.
5. Tests: fast unit tests inside a module; DB/integration tests (`@PosIntegrationTest`) in `app`.
6. A module-boundary test (ArchUnit or a Gradle check) fails the build if `internal` is used from another module.

## Steps (each ends with `./gradlew test` green and the app running)
| # | Step | Risk |
|---|---|---|
| 1 | Create root build + `platform` module; move `common` | Low |
| 2 | Create `app` module; move `ServerApplication`, `config`, resources, migrations, tests; `bootJar` -> `server.jar` (desktop + e2e paths unchanged) | Medium: build paths |
| 3 | Extract leaf modules: `audit`, `settings`, `catalog` | Low |
| 4 | Extract `identity`, `cash`, `menu` | Low |
| 5 | Add `SaleCompleted` event; extract `inventory` as a listener | Medium: transaction semantics (must stay one DB transaction) |
| 6 | Extract `sales`, then `receipt` | Medium |
| 7 | `api` / `internal` split + boundary test | Low |
| 8 | Update `pos-backend` skill, `architecture.md`, `pos-feature` skeletons, CI | Low |

## Alternatives considered
- **Spring Modulith** (logical modules, single Gradle project): less build churn, boundaries checked by a test. Good fallback if the multi-module build proves too heavy, but the compiler does not enforce it.
- **Microservices**: no. One terminal, offline, low-end hardware.

## Not changing
API URLs, database schema, UI and desktop code, `server.jar` name and packaging.

## Outcome
- 11 Gradle projects: `platform`, 9 feature modules, `app`. Package names unchanged.
- `-parameters` must be set for all modules (root `build.gradle`); the Boot plugin only adds it to `app`. Without it Spring Data named query parameters fail at runtime (caught by the tests).
- `SaleCompleted` event replaced `sales -> inventory`.
- Jar moved to `server/app/build/libs/server.jar` (desktop dev path, electron-builder, e2e stack updated).
- Verified: 40 server tests, desktop smoke test, e2e `sale-cycle` and `line-discount`.
