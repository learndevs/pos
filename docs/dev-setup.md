# Developer Setup

## Prerequisites
- JDK 21 or newer (builds Java 21 bytecode).
- PostgreSQL 17 running locally on port 5432.

## Database (one time)
```sql
CREATE ROLE pos_app LOGIN PASSWORD '<choose one>';
CREATE DATABASE pos OWNER pos_app ENCODING 'UTF8';       -- dev
CREATE DATABASE pos_test OWNER pos_app ENCODING 'UTF8';  -- tests, wiped on every test run
```
Then copy `server/config/application-local.properties.example` to `server/config/application-local.properties` and set the password. The real file is git-ignored and only loaded by profile `local` (used by `bootRun`), never by tests.

## Whole app (dev)
```bash
npx npm@11 install      # npm 10.9 crashes on this dependency tree
npm run dev             # server :8080 + Vite :5173 + Electron window
```
First launch shows the owner setup screen; after that, sign in with a user tile and PIN.

## Server
```bash
cd server
./gradlew bootRun        # applies Flyway migrations to `pos`, serves on 127.0.0.1:8080
./gradlew test           # rebuilds `pos_test` and runs unit + schema rule tests
curl http://127.0.0.1:8080/actuator/health
```

## Notes
- Do not point tests at the `pos` database: the test setup runs `flyway clean`.
- Homebrew Postgres often trusts local connections, so a wrong password may still connect locally. The packaged app uses password auth.
