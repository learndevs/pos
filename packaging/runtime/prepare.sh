#!/usr/bin/env bash
# Prepares everything an installer needs for one target: UIs, server jars, Java runtime.
# PostgreSQL is NOT bundled: install it as its own service (see packaging/database/README.md).
# Usage: prepare.sh [target]   (default: this machine)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
case "$(uname -s)-$(uname -m)" in
  Darwin-arm64) HOST=mac-arm64 ;; Darwin-x86_64) HOST=mac-x64 ;; MINGW*|MSYS*|CYGWIN*) HOST=win-x64 ;; *) HOST=unknown ;;
esac
TARGET="${1:-$HOST}"
echo "== prepare $TARGET"
(cd "$ROOT/ui" && npx vite build >/dev/null) && echo "ui built"
(cd "$ROOT/admin-ui" && npx vite build >/dev/null) && echo "admin-ui built"
(cd "$ROOT/server" && ./gradlew :app:bootJar :admin-app:bootJar -q --console=plain -Dorg.gradle.jvmargs=-Xmx1g) && echo "server.jar + admin.jar built"
(cd "$ROOT/desktop" && npm run build:ts --silent) && echo "desktop compiled"
bash "$ROOT/packaging/runtime/build-jre.sh" "$TARGET"
