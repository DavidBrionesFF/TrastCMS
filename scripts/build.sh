#!/usr/bin/env sh
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -f "$ROOT/tools/graalvm/bin/java" ]; then
    export JAVA_HOME="$ROOT/tools/graalvm"
    export PATH="$JAVA_HOME/bin:$PATH"
    printf '[INFO] Usando GraalVM JDK local desde tools/graalvm\n'
fi

./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

mkdir -p release dist
JAR_FILE=$(find target -maxdepth 1 -type f -name 'trastcms-*.jar' | head -n 1)
if [ -n "$JAR_FILE" ]; then
    cp "$JAR_FILE" release/
    cp "$JAR_FILE" dist/
    printf '\n[OK] JAR generado y copiado exitosamente a release/\n'
else
    printf '\nJAR generado en target/\n'
fi
