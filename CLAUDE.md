# POS project notes
Offline Windows restaurant POS: Electron + React UI (`ui/`, `desktop/`) and Spring Boot + PostgreSQL (`server/`).

**Start here:** `docs/architecture.md` (how requests flow, where code goes), then the `pos-feature` skill before building anything.
Feature specs: `docs/features/`. Decisions: `docs/decisions.md`. Area skills live in `.claude/skills/`.

Hard rules: money = integer minor units; finished sales are immutable (refund instead); stock/cash/audit are append-only;
API binds to 127.0.0.1 only; business rules live on the server; UI reaches devices only via the preload bridge.

Verify by running: `cd server && ./gradlew test`, UI/desktop typecheck, and `tools/e2e/test-stack.sh up` + a flow script.
Tests stay light (one flow test per feature); never point tests or e2e at the dev DB `pos`.
