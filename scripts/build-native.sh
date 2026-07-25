#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v native-image >/dev/null 2>&1; then
  echo "Error: native-image no está disponible. Instale GraalVM 25+ y configure JAVA_HOME." >&2
  exit 1
fi

./mvnw --batch-mode --no-transfer-progress clean -Pnative -DskipTests native:compile

mkdir -p dist
executable="$(find target -maxdepth 1 -type f -name 'trastcms-*' ! -name '*.jar' -perm -111 | head -n 1)"
if [[ -z "$executable" ]]; then
  echo "Error: Maven terminó pero no se encontró el ejecutable nativo." >&2
  exit 1
fi

cp "$executable" "dist/$(basename "$executable")"
echo "Ejecutable nativo: $ROOT/dist/$(basename "$executable")"
