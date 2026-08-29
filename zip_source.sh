#!/bin/bash
# =============================================================================
# zip_source.sh — Comprime el código fuente del proyecto en un ZIP
#
# Uso: Ejecutar desde Git Bash en la raíz del proyecto.
#      El ZIP se guarda en la carpeta padre del proyecto.
#
# Estructura del proyecto (TrastCMS):
#   raíz/         → Spring Boot (Java/Maven)
#   frontend/     → Vue 3 + Vite
#   plugins/      → plugins incorporados (TrastCRM, TrastPay, TrastStore, TrastSaaS)
#
# Se excluyen (basado en .gitignore):
#   Carpetas    : target  node_modules  dist  .vite  data  .git  .idea  .vscode  .mvn
#   Archivos    : .env  .env.*  (excepto .env.example)  *.class  *.jar  *.log
#                *.db  *.mv.db  *.trace.db  *.iml  .DS_Store  Thumbs.db
# =============================================================================

set -e

# ── Función para convertir ruta Unix (Git Bash) a ruta Windows ──────────────
#    /c/Users/... → C:\Users\...
unix_to_win() {
  local path="$1"
  path="${path#/}"                # quitar / inicial     → c/Users/...
  local drive="${path:0:1}"       # letra de unidad       → c
  local rest="${path:1}"          # resto de la ruta      → /Users/...
  echo "${drive^^}:${rest//\//\\}" # C:\Users\...
}

# ── Directorios y nombres ───────────────────────────────────────────────────
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_NAME="$(basename "$PROJECT_DIR")"
OUTPUT_DIR="$(dirname "$PROJECT_DIR")"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
ZIP_NAME="${OUTPUT_DIR}/${PROJECT_NAME}_source_${TIMESTAMP}.zip"
SCRIPT_NAME="$(basename "$0")"

# Convertir a rutas Windows para PowerShell/.NET
WIN_PROJECT_DIR="$(unix_to_win "$PROJECT_DIR")"
WIN_ZIP_NAME="$(unix_to_win "$ZIP_NAME")"

# ── Carpetas a excluir (coincidencia por nombre en cualquier nivel) ──────────
EXCLUDE_DIRS=(
  ".git"
  ".idea"
  ".vscode"
  ".mvn"
  ".vite"
  "target"
  "node_modules"
  "dist"
  "data"
  "tools"
  "$SCRIPT_NAME"
)

# ── Extensiones y nombres de archivo a excluir ───────────────────────────────
EXCLUDE_EXTS=(
  ".class"
  ".jar"
  ".log"
  ".db"
  ".mv.db"
  ".trace.db"
  ".iml"
  ".DS_Store"
  "Thumbs.db"
)

# Construir arrays para PowerShell
ps_dirs=""
for d in "${EXCLUDE_DIRS[@]}"; do
  ps_dirs+="'$d',"
done
ps_dirs="${ps_dirs%,}"

ps_exts=""
for e in "${EXCLUDE_EXTS[@]}"; do
  ps_exts+="'$e',"
done
ps_exts="${ps_exts%,}"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  TrastCMS — Empaquetado de código fuente"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Proyecto  : $WIN_PROJECT_DIR"
echo "  Salida    : $WIN_ZIP_NAME"
echo "  Excluyendo directorios : ${EXCLUDE_DIRS[*]}"
echo "  Excluyendo extensiones : ${EXCLUDE_EXTS[*]}"
echo "  Excluyendo secretos    : .env  .env.*  (se conserva .env.example)"
echo ""

# ── Crear el ZIP con PowerShell ─────────────────────────────────────────────
powershell -NoProfile -Command "

  Add-Type -AssemblyName System.IO.Compression.FileSystem

  \$zip      = [System.IO.Compression.ZipFile]::Open('$WIN_ZIP_NAME', 'Create')
  \$root     = '$WIN_PROJECT_DIR'
  \$rootLen  = \$root.Length
  \$exclDirs = @($ps_dirs)
  \$exclExts = @($ps_exts)
  \$count    = 0
  \$skipped  = 0

  Get-ChildItem -Path \$root -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
    \$relative = \$_.FullName.Substring(\$rootLen + 1)
    \$skip = \$false

    # 1. Excluir si alguna carpeta en la ruta está en la lista de exclusión
    foreach (\$part in (\$relative -split '[\\\\\/]')) {
      if (\$exclDirs -contains \$part) {
        \$skip = \$true
        break
      }
    }

    # 2. Excluir por extensión
    if (-not \$skip) {
      \$ext = [System.IO.Path]::GetExtension(\$_.Name).ToLower()
      if (\$exclExts -contains \$ext) {
        \$skip = \$true
      }
    }

    # 3. Excluir .env y .env.* (pero NO .env.example)
    if (-not \$skip) {
      \$fname = \$_.Name
      if (\$fname -eq '.env' -or (\$fname -like '.env.*' -and \$fname -ne '.env.example')) {
        \$skip = \$true
      }
    }

    if (\$skip) {
      \$skipped++
    } else {
      try {
        \$entryPath = \$relative.Replace('\', '/')
        \$entry = \$zip.CreateEntry(\$entryPath, [System.IO.Compression.CompressionLevel]::Optimal)
        \$fs    = [System.IO.File]::OpenRead(\$_.FullName)
        \$es    = \$entry.Open()
        \$fs.CopyTo(\$es)
        \$fs.Close()
        \$es.Close()
        \$count++
      }
      catch {
        Write-Host \"WARN: No se pudo agregar \$relative — \$_\"
      }
    }
  }

  \$zip.Dispose()
  Write-Host \"\"
  Write-Host \"  Archivos incluidos : \$count\"
  Write-Host \"  Archivos excluidos : \$skipped\"
"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ZIP creado: $WIN_ZIP_NAME"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
