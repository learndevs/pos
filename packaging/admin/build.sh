#!/usr/bin/env bash
# Builds admin.jar (with the admin UI inside) and copies it next to the Dockerfile.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$HERE/../.."
(cd "$ROOT/admin-ui" && npx vite build >/dev/null)
(cd "$ROOT/server" && ./gradlew :admin-app:bootJar -q --console=plain)
cp "$ROOT/server/admin-app/build/libs/admin.jar" "$HERE/admin.jar"
[ -f "$HERE/.env" ] || { echo "ADMIN_DB_PASSWORD=$(openssl rand -hex 16)" > "$HERE/.env"; echo "created .env with a random database password"; }
echo "ready: docker compose up -d"
