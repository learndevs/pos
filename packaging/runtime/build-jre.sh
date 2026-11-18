#!/usr/bin/env bash
# Builds a small Java runtime (jlink) for the server jars into desktop/resources/jre/<target>.
# jlink packs the JDK it runs from, so build each target on its own OS (CI does this); a cross-build needs
# TARGET_JMODS=<path to the target JDK's jmods>.
# Usage: build-jre.sh <win-x64|mac-arm64|mac-x64>
set -euo pipefail
TARGET="${1:?target: win-x64 | mac-arm64 | mac-x64}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/desktop/resources/jre/$TARGET"
if [ -d "$OUT/bin" ]; then echo "jre $TARGET already present"; exit 0; fi
JLINK="${JAVA_HOME:+$JAVA_HOME/bin/}jlink"
MODULES=java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.management,jdk.unsupported,jdk.zipfs
rm -rf "$OUT"; mkdir -p "$(dirname "$OUT")"
"$JLINK" ${TARGET_JMODS:+--module-path "$TARGET_JMODS"} --add-modules "$MODULES" \
  --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output "$OUT"
echo "jre $TARGET -> $OUT ($(du -sh "$OUT" | cut -f1))"
