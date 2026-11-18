---
name: pos-backend
description: Conventions for the Spring Boot server in server/: project layout, DB connection, Flyway migrations, security, testing and how to run it. Use when adding or changing endpoints, entities, migrations, or backend tests.
---

# Backend conventions (server/)

## Stack
Spring Boot 4.1, Java 21 bytecode (`options.release = 21`; any JDK >= 21 builds it), Gradle wrapper, PostgreSQL 17, Flyway, Spring Data JPA, Spring Security, Bean Validation, Actuator.

## Layout: Gradle multi-module (one module per functionality)
```
server/
  build.gradle, settings.gradle     shared config (Java 21, Spring BOM, -parameters), module list
  platform/                         BaseEntity, UuidV7, PosException; exposes JPA/web/validation/security starters
  modules/<feature>/                audit, settings, catalog, identity, cash, menu, inventory, sales, receipt
  app/                              ServerApplication, config (security, filters, Clock), application.yml,
                                    ALL Flyway migrations (db/migration, db/demo), integration tests, bootJar -> server.jar
```
- Package names are unchanged: module `modules/sales` holds `com.retailpos.server.sales`.
- A module may only use what its `build.gradle` declares (`api project(':modules:x')`). Allowed directions:
  `platform <- audit <- identity <- cash <- sales <- receipt`, `catalog <- menu, sales`, `settings <- sales, receipt`, `sales <- inventory`. No cycles.
- Need something from a module you do not depend on? Prefer an **event** published by the owner (example: `SaleCompleted` from sales, handled by `inventory.InventoryService.onSaleCompleted`, synchronous in the same DB transaction). Do not add a dependency that creates a cycle.
- New feature module: `modules/<name>/build.gradle` (+ `include` in `settings.gradle`, + `implementation project(...)` in `app/build.gradle`).
- Each module owns its entity, repository, service, controller and DTOs. Cross-module calls go through the other module's service (read-only lookups via its repository are OK).

## Data model conventions
- One JPA entity per table, in its feature package. Entities with UUID ids extend `common/BaseEntity` (assigns UUIDv7, implements `Persistable` so `save()` INSERTs instead of SELECT+merge). New rows are created via constructors/static factories that call `super(true)`.
- Aggregates own their children: `PosTransaction` -> `BasketItem` (JOINED inheritance on `basket_item`, discriminator `kind`) -> `SaleItem` / `TenderItem`; `SaleItem` -> `SaleItemModifier`. Change basket items only through `PosTransaction` methods so totals stay right.
- Business rules live on entities (`PosTransaction.addTender`, `SaleItem.price`); services load, call, save, and coordinate across features (receipt number, stock).
- Rule failures: throw `common/PosException` (409/400/404 with a cashier-readable message).
- Map ids of other aggregates as plain `UUID` columns (e.g. `createdBy`, `cashSessionId`), not `@ManyToOne`, unless you need the object.
- `columnDefinition = "jsonb"` + `@JdbcTypeCode(SqlTypes.JSON)` for jsonb columns. Enums: `@Enumerated(STRING)` matching the DB CHECK values.
- Native SQL only where JPA cannot express it (e.g. `nextReceiptNo` upsert ... RETURNING, recipe projection). No `JdbcTemplate` in main code.

## Database connection
- Dev DB: `pos` on `localhost:5432`, role `pos_app` (owner). Test DB: `pos_test`, same role.
- Dev credentials live in `server/config/application-local.properties` (git-ignored; copy from the `.example`), loaded only by profile `local`, which `bootRun` and the Electron dev launch activate. Env vars `DB_URL`, `DB_USER`, `DB_PASSWORD` also work. Never commit a password.
- Never put DB settings in `server/config/application.properties`: external files outrank the test profile, so tests would hit (and clean) the dev DB.
- Packaged app: Electron starts bundled Postgres on 127.0.0.1 with a random port and a generated password, and passes them as env vars.
- Hikari pool max 5. `open-in-view: false`. JDBC time zone UTC.

## Two apps, shared jars
- `app` = POS server (`server.jar`), `admin-app` = admin backend (`admin.jar`, :8081, DB `pos_admin`).
- Both use the same `platform`, `catalog`, `master-schema`, `sync-contract` jars: one definition of master data and its rules.
  Entity create/update methods (`Product.create/update`, `Category`, `TaxRate`) are the only way to change master data, on both sides.
- `admin-app` needs explicit `@EntityScan` / `@EnableJpaRepositories("com.retailpos.server")` (its main class is in `...admin`).
- Never send entities over the wire: use `sync-contract` records + `CatalogMapper`; the zip format is `PackageZip` (write on admin, read on till).
- Contract changes inside v1 are additive only; breaking changes bump `Contract.CONTRACT_VERSION` (tills refuse newer).
- Adding an entity to sync (e.g. menus): record it in the change log on admin edits, add a record + list to `PackageContent`
  + file in `PackageZip`, apply it in `SyncApplier` (references first), add the shared table to `db/master`.

## Migrations
- **One global version sequence; the folder decides which DB runs it**: `app/.../db/migration` (POS only),
  `modules/master-schema/.../db/master` (POS **and** admin, shared master tables), `admin-app/.../db/admin` (admin only),
  `db/demo` / `db/admin-demo` (dev data). Use the next free number across all folders.
- Flyway owns the schema; `ddl-auto: validate`. Files `V<n>__<desc>.sql` in `server/app/src/main/resources/db/migration` (one history for all modules; start each file with `-- Module: <name>`).
- Never edit a migration once it is committed and applied anywhere; add a new one.
- Demo data (dev only, profile `local`) lives in `db/demo` as `V<n>.1__...`. Tests never load it. Give new demo files a version above the latest schema migration, or Flyway rejects them as out of order.
- Schema reference: `docs/database-schema.md`. Rules from `pos-domain` that the DB enforces must keep a test in `SchemaRulesTest`.

## Code rules
- Layers: controller (DTOs, validation) -> service (`@Transactional`, business rules) -> repository. Never expose entities over REST.
- IDs: `UuidV7.next()` assigned in the app before insert; no DB defaults or sequences.
- Money: `long` minor units. Enums stored as text matching the DB CHECK values.
- Errors: RFC 7807 problem responses; no stack traces to the UI.
- Security (`config/SecurityConfig`):
  - `LaunchTokenFilter`: if `pos.launch-token` (env `POS_LAUNCH_TOKEN`, set by Electron) is set, every `/api/**` call needs header `X-POS-Launch-Token`. Empty in plain dev.
  - `identity/SessionAuthFilter`: `Authorization: Bearer <session>` from `POST /api/auth/login`; in-memory `SessionStore`, 12 h idle timeout.
  - Authorities = `ROLE_<code>` + each `role_permission`; guard with `@PreAuthorize("hasAuthority('ORDER_VOID')")`.
  - Public: health, static UI, `/api/setup/**`, `/api/auth/login`, `/api/auth/users`. Other `/api/**` need a session. Everything else is denied.
  - PINs are BCrypt hashed, 4-8 digits; 5 wrong PINs lock the user for 5 minutes; logins are audited.
- Tests that assert with `JdbcTemplate` must `em.flush()` first: JDBC reads do not flush pending JPA writes.
- Time: inject `Clock`, never call `Instant.now()` directly.
- Logging: no PII, PINs or tokens.
- Backups: `pg_dump` to the configured folder; never copy the live data dir.

## Run and test
- `cd server && ./gradlew bootRun` (runs `:app`, working dir `server/` so `server/config/` is used), then `curl http://127.0.0.1:8080/actuator/health`. The server binds to 127.0.0.1 only; `SERVER_PORT` overrides 8080.
- `./gradlew test` (all modules): integration tests live in `app/src/test` and use `@PosIntegrationTest` (profile `test` -> `pos_test`; `TestDatabaseConfig` cleans and re-migrates, and refuses any DB not ending in `_test`). Add `@Transactional` to roll back; Postgres aborts a transaction after an error, so keep one rejected statement per test.
- Keep tests light for now: one flow test per feature, not exhaustive suites.
- JVM flags for the packaged build start at `-Xmx256m -XX:+UseSerialGC`; measure before changing.
