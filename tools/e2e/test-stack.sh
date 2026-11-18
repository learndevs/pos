#!/usr/bin/env bash
# Throwaway test stack for UI checks. Uses the pos_test database ONLY.
set -euo pipefail
command -v psql >/dev/null || export PATH="/opt/homebrew/opt/postgresql@17/bin:$PATH"   # Homebrew Postgres
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG="${TMPDIR:-/tmp}/pos-e2e"; mkdir -p "$LOG"
API=http://127.0.0.1:8090

down() {
  pkill -f "remote-debugging-port=9333" 2>/dev/null || true
  pkill -f "vite --port 5174" 2>/dev/null || true
  pkill -f "SERVER_PORT=8090" 2>/dev/null || true
  pkill -f "pos-e2e-server" 2>/dev/null || true
  lsof -ti tcp:8090 2>/dev/null | xargs kill 2>/dev/null || true
}

wait_for() { for _ in $(seq 1 120); do curl -s -o /dev/null "$1" && return 0; sleep 1; done; echo "timeout: $1" >&2; exit 1; }

up() {
  down
  echo "reset pos_test schema"
  psql -h localhost -d pos_test -qc "DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION pos_app;" >/dev/null
  echo "build server"
  (cd "$ROOT/server" && ./gradlew bootJar -q --console=plain -Dorg.gradle.jvmargs=-Xmx1g)
  echo "start server :8090 (demo data)"
  (cd "$ROOT/server" && DB_URL=jdbc:postgresql://localhost:5432/pos_test DB_USER=pos_app SERVER_PORT=8090 \
     SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/master,classpath:db/demo \
     nohup java -Dpos-e2e-server -jar app/build/libs/server.jar > "$LOG/server.log" 2>&1 &)
  echo "start ui :5174"
  (cd "$ROOT/ui" && POS_SERVER_URL=$API nohup npx vite --port 5174 --strictPort > "$LOG/vite.log" 2>&1 &)
  wait_for "$API/actuator/health"; wait_for http://localhost:5174
  echo "seed: owner check/1234, open day"
  curl -s -o /dev/null -X POST $API/api/setup/owner -H 'Content-Type: application/json' -d '{"username":"check","displayName":"Check","pin":"1234"}'
  TOK=$(curl -s -X POST $API/api/auth/login -H 'Content-Type: application/json' -d '{"username":"check","pin":"1234"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
  curl -s -o /dev/null -X POST $API/api/cash/session/open -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' -d '{"openingFloatMinor":5000}'
  echo "start electron (CDP :9333)"
  (cd "$ROOT/desktop" && npm run build:ts --silent && env -u ELECTRON_RUN_AS_NODE POS_DEV=1 POS_USER_DATA="$LOG/electron-data" POS_SERVER_URL=$API POS_UI_URL=http://localhost:5174 \
     nohup node scripts/run-electron.cjs --dev --remote-debugging-port=9333 > "$LOG/electron.log" 2>&1 &)
  wait_for http://127.0.0.1:9333/json
  echo "ready. logs: $LOG"
}

case "${1:-}" in
  up) up ;;
  down) down; echo "stopped" ;;
  *) echo "usage: $0 up|down"; exit 2 ;;
esac
