# POS server

Spring Boot 4 local API for the POS. Java 21 bytecode, PostgreSQL, Flyway.

## Run
1. Copy `config/application-local.properties.example` to `config/application-local.properties` (git-ignored) and set the DB password. `bootRun` uses profile `local`.
   Env vars `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` also work.
2. `./gradlew bootRun`, then check `curl http://127.0.0.1:8080/actuator/health`.

## Test
`./gradlew test` (profile `test`) rebuilds the `pos_test` database from the migrations (clean + migrate) and runs the schema rule tests.

## Layout
```
src/main/java/com/retailpos/server/
  common/     UUIDv7, money, errors
  config/     security and Spring config
  identity/   users, roles, PIN login
  settings/   app_setting
  catalog/    products, categories, tax, modifiers, tables
  inventory/  stock items, recipes, stock ledger
  cash/       cash sessions, cash movements, Z-report
  sales/      transactions, basket items (sale/tender), refunds
  printing/   ESC/POS, print queue
  reporting/  report queries
  audit/      audit_log
src/main/resources/db/migration/   Flyway migrations (V1 schema, V2 seed)
src/test/java/.../schema/          DB integrity rule tests
```
Each feature package holds its own entity, repository, service and controller (package by feature, not by layer).
