#!/usr/bin/env bash
# ==============================================================================
# Script de Desarrollo Frontend en Vivo (Vite Dev Server + Proxy a Spring Boot)
# ==============================================================================
# Permite ejecutar Vue con Hot Module Replacement (HMR) conectado al backend.
#
# Uso:
#   ./dev.sh                         (Usa VITE_API_URL del .env o http://localhost:8080)
#   ./dev.sh http://localhost:8080   (Especifica la URL del backend)
#   ./dev.sh http://localhost:8080 5173 (Especifica la URL del backend y puerto frontend)
# ==============================================================================

# Cargar variables de .env si existe
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Parámetros opcionales
TARGET_API="${1:-${VITE_API_URL:-http://localhost:8080}}"
DEV_PORT="${2:-${VITE_PORT:-5173}}"

export VITE_API_URL="$TARGET_API"
export VITE_PORT="$DEV_PORT"

echo "===================================================================="
echo " 🚀 Iniciando Servidor de Desarrollo Frontend (Vite)"
echo "===================================================================="
echo " 🌐 Servidor Frontend:  http://localhost:${VITE_PORT}"
echo " 🔗 Backend API Target: ${VITE_API_URL}"
echo " ⚡ Cambios en vivo (HMR) habilitados sin recompilar Spring Boot."
echo "===================================================================="
echo ""

cd frontend || exit 1
npm run dev
