# POS server

Spring Boot 4 local API for the POS. Java 21 bytecode, PostgreSQL, Flyway.

## Run
1. Copy `config/application-local.properties.example` to `config/application-local.properties` (git-ignored) and set the DB password. `bootRun` uses profile `local`.
   Env vars `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` also work.
2. `./gradlew bootRun`, then check `curl http://127.0.0.1:8080/actuator/health`.

## Test
`./gradlew test` runs every module; integration tests (profile `test`, in `app/`) rebuild the `pos_test` database from the migrations (clean + migrate) and runs the schema rule tests.

## Layout (Gradle multi-module)
```
platform/             shared kernel: BaseEntity, UuidV7, PosException
modules/audit         audit_log
modules/settings      app_setting, PosSettings
modules/catalog       products, categories, tax, search
modules/identity      users, roles, PIN login, sessions
modules/cash          day start (cash sessions)
modules/menu          sale / tender / item menus, pos_function
modules/inventory     stock ledger; listens to SaleCompleted
modules/sales         transaction aggregate, tenders, discounts
modules/receipt       receipt templates + rendering
modules/master-schema shared master-data migrations (db/master, run by POS and admin)
modules/sync-contract shared JSON records + PackageZip (admin <-> POS)
modules/sync-server   admin: change log, packages, terminals, archive
modules/sync-client   POS: fetch/apply packages, transaction outbox
app/                  POS Spring Boot app -> build/libs/server.jar
admin-app/            admin Spring Boot app -> build/libs/admin.jar (serves admin-ui)
```
Rules and how to add a module: `.claude/skills/pos-backend/SKILL.md`.
