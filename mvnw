#!/usr/bin/env sh
set -eu
MAVEN_VERSION=3.9.12
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.zip"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  printf 'Maven no está instalado. Descargando Apache Maven %s...\n' "$MAVEN_VERSION"
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then wget -O "$ARCHIVE" "$URL"
  else echo 'Se requiere curl o wget para descargar Maven.' >&2; exit 1; fi
  command -v unzip >/dev/null 2>&1 || { echo 'Se requiere unzip para preparar Maven.' >&2; exit 1; }
  unzip -q -o "$ARCHIVE" -d "$BASE_DIR/.mvn"
  rm -f "$ARCHIVE"
fi
exec "$MAVEN_HOME/bin/mvn" "$@"
