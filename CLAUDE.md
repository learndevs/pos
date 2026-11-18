# POS project notes
Offline Windows restaurant POS. Read `docs/tech-spec.md` before architectural changes.
Project skills live in `.claude/skills/`; use the matching one for the area you touch.
When a decision changes, update `docs/decisions.md` (see `pos-docs` skill).
Hard rules: money = integer minor units; sales/stock are append-only ledgers; API binds to 127.0.0.1 only.
Server: `cd server && ./gradlew test` before finishing backend work (needs local Postgres; see docs/dev-setup.md).
