#!/usr/bin/env bash
# =============================================================================
# build-native.sh — Compila TrastCMS como ejecutable nativo con GraalVM
#
# Uso:
#   ./scripts/build-native.sh                  # compilación completa
#   ./scripts/build-native.sh --skip-frontend  # omite build de Vue
#   ./scripts/build-native.sh --skip-tests     # omite tests (activado por defecto)
#   ./scripts/build-native.sh --help
#
# Resolución de GraalVM (en orden de prioridad):
#   1. tools/graalvm/  dentro del proyecto  ← recomendado para equipos
#   2. Variable GRAALVM_HOME
#   3. Variable JAVA_HOME (si apunta a GraalVM)
#   4. native-image disponible en PATH
#
# Para usar GraalVM local:
#   mkdir -p tools/graalvm
#   # Descomprime GraalVM JDK 25+ dentro de tools/graalvm/
#   # Estructura esperada: tools/graalvm/bin/native-image
#
# Requisitos:
#   - GraalVM JDK 25+
#   - Maven Wrapper (mvnw) en la raíz del proyecto
#   - En Linux: build-essential / glibc-devel
#   - Memoria recomendada: 8 GB libres
# =============================================================================
set -euo pipefail

# ── Colores ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}${BOLD}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}${BOLD}[ OK ]${RESET}  $*"; }
warn()    { echo -e "${YELLOW}${BOLD}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}${BOLD}[ERR ]${RESET}  $*" >&2; }

# ── Directorio raíz ──────────────────────────────────────────────────────────
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# ── Argumentos ───────────────────────────────────────────────────────────────
SKIP_FRONTEND=false
SKIP_TESTS=true   # nativo siempre omite tests por defecto

for arg in "$@"; do
  case "$arg" in
    --skip-frontend) SKIP_FRONTEND=true ;;
    --skip-tests)    SKIP_TESTS=true ;;
    --with-tests)    SKIP_TESTS=false ;;
    --help|-h)
      echo "Uso: $0 [--skip-frontend] [--skip-tests] [--with-tests]"
      exit 0 ;;
    *) warn "Argumento desconocido: $arg" ;;
  esac
done

# ── Detectar OS y arquitectura para nombre del artefacto ─────────────────────
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"
case "$ARCH" in
  x86_64|amd64) ARCH="x64" ;;
  aarch64|arm64) ARCH="arm64" ;;
esac
PLATFORM="${OS}-${ARCH}"

# ── Leer versión desde pom.xml ───────────────────────────────────────────────
VERSION="$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')"
ARTIFACT_NAME="trastcms-${VERSION}-${PLATFORM}"

echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}  TrastCMS — Compilación Nativa GraalVM${RESET}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""

# ── Resolver GraalVM (local → GRAALVM_HOME → JAVA_HOME → PATH) ───────────────
info "Buscando GraalVM..."

GRAALVM_BIN=""

# 1. tools/graalvm/ dentro del proyecto (recomendado para equipos)
LOCAL_GRAALVM="${ROOT}/tools/graalvm"
if [[ -f "${LOCAL_GRAALVM}/bin/native-image" ]]; then
  GRAALVM_BIN="${LOCAL_GRAALVM}/bin"
  success "GraalVM local encontrado en tools/graalvm/"

# 2. Variable de entorno GRAALVM_HOME
elif [[ -n "${GRAALVM_HOME:-}" && -f "${GRAALVM_HOME}/bin/native-image" ]]; then
  GRAALVM_BIN="${GRAALVM_HOME}/bin"
  success "GraalVM encontrado via GRAALVM_HOME"

# 3. JAVA_HOME apuntando a GraalVM
elif [[ -n "${JAVA_HOME:-}" && -f "${JAVA_HOME}/bin/native-image" ]]; then
  GRAALVM_BIN="${JAVA_HOME}/bin"
  success "GraalVM encontrado via JAVA_HOME"

# 4. native-image en el PATH del sistema
elif command -v native-image >/dev/null 2>&1; then
  GRAALVM_BIN="$(dirname "$(command -v native-image)")"
  success "GraalVM encontrado en PATH del sistema"

else
  error "GraalVM no encontrado. Opciones:"
  error ""
  error "  OPCIÓN A — GraalVM local al proyecto (recomendada):"
  error "    mkdir -p ${ROOT}/tools/graalvm"
  error "    # Descarga GraalVM JDK 25+ y descomprímelo en tools/graalvm/"
  error "    # https://graalvm.org/downloads/"
  error ""
  error "  OPCIÓN B — Variable de entorno:"
  error "    export GRAALVM_HOME=/ruta/a/graalvm"
  error ""
  error "  OPCIÓN C — Agregar GraalVM al PATH del sistema:"
  error "    export PATH=/ruta/a/graalvm/bin:\$PATH"
  exit 1
fi

# Agregar GraalVM al PATH de esta sesión
export PATH="${GRAALVM_BIN}:${PATH}"
export JAVA_HOME="$(dirname "${GRAALVM_BIN}")"

success "GraalVM detectado: $(native-image --version 2>&1 | head -n1)"
success "Java: $(java -version 2>&1 | head -n1)"
info "Plataforma  : $PLATFORM"
info "Versión     : $VERSION"
info "Artefacto   : $ARTIFACT_NAME"
info "Skip tests  : $SKIP_TESTS"
info "Skip frontend: $SKIP_FRONTEND"
echo ""

# ── Variables Maven ───────────────────────────────────────────────────────────
# Limita el heap del propio Maven; native-image administra su propia memoria
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m}"

MVNW="./mvnw"
MVN_ARGS=(
  --batch-mode
  --no-transfer-progress
  -Pnative
  clean
  native:compile
)

if [[ "$SKIP_TESTS" == "true" ]]; then
  MVN_ARGS+=(-DskipTests)
fi

if [[ "$SKIP_FRONTEND" == "true" ]]; then
  MVN_ARGS+=(-Pfrontend-only -Dfrontend.skip=true)
fi

# ── Compilar ──────────────────────────────────────────────────────────────────
info "Iniciando compilación nativa (puede tardar 5-15 minutos)..."
echo ""
START_TS="$(date +%s)"

"$MVNW" "${MVN_ARGS[@]}"

END_TS="$(date +%s)"
ELAPSED=$(( END_TS - START_TS ))
echo ""
success "Compilación completada en ${ELAPSED}s"

# ── Empaquetar artefacto ──────────────────────────────────────────────────────
info "Buscando ejecutable nativo en target/..."
EXECUTABLE="$(find target -maxdepth 1 -type f -name 'trastcms-*' ! -name '*.jar' -perm -111 2>/dev/null | head -n1)"

if [[ -z "$EXECUTABLE" ]]; then
  error "No se encontró el ejecutable nativo en target/."
  error "Revise la salida de Maven para detectar el error."
  exit 1
fi

mkdir -p dist release
DEST="dist/${ARTIFACT_NAME}"
RELEASE_DEST="release/${ARTIFACT_NAME}"
cp "$EXECUTABLE" "$DEST"
cp "$EXECUTABLE" "$RELEASE_DEST"
chmod +x "$DEST" "$RELEASE_DEST"

# SHA-256 del binario
sha256sum "$DEST" > "${DEST}.sha256" 2>/dev/null || shasum -a 256 "$DEST" > "${DEST}.sha256"
cp "${DEST}.sha256" "${RELEASE_DEST}.sha256"

echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
success "Ejecutable : ${ROOT}/${DEST}"
success "Checksum   : ${ROOT}/${DEST}.sha256"
echo -e "  Tamaño   : $(du -sh "$DEST" | cut -f1)"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo ""
info "Para ejecutar:"
echo "  ./${DEST}"
echo ""
