<#
.SYNOPSIS
    Compila TrastCMS como ejecutable nativo con GraalVM en Windows.

.DESCRIPTION
    Script PowerShell para compilar el ejecutable nativo de TrastCMS.

    Resolución de GraalVM (en orden de prioridad):
      1. tools\graalvm\  dentro del proyecto  <- recomendado para equipos
      2. Variable GRAALVM_HOME
      3. Variable JAVA_HOME (si apunta a GraalVM)
      4. native-image disponible en PATH

    Para usar GraalVM local:
      New-Item -ItemType Directory -Force tools\graalvm
      # Descomprime GraalVM JDK 25+ dentro de tools\graalvm\
      # Estructura esperada: tools\graalvm\bin\native-image.cmd

    IMPORTANTE: En Windows, GraalVM requiere cl.exe de Visual Studio.
    Ejecutar desde "x64 Native Tools Command Prompt for VS 2022"
    o asegurarse de que cl.exe esté en el PATH.

.PARAMETER SkipFrontend
    Omite la compilacion del frontend Vue (Vite).

.PARAMETER WithTests
    Ejecuta los tests durante la compilacion (por defecto se omiten).

.EXAMPLE
    .\scripts\build-native.ps1
    .\scripts\build-native.ps1 -SkipFrontend
    .\scripts\build-native.ps1 -SkipFrontend -WithTests

.NOTES
    Requisitos:
    - GraalVM JDK 25+
    - Visual Studio Build Tools 2022 con "Desktop development with C++"
    - Maven Wrapper (mvnw.cmd) en la raiz del proyecto
    - Memoria recomendada: 8 GB libres
#>
param(
    [switch]$SkipFrontend,
    [switch]$WithTests
)

$ErrorActionPreference = 'Stop'

# ── Helpers de salida coloreada ───────────────────────────────────────────────
function Write-Info    { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Ok      { param($msg) Write-Host "[ OK ]  $msg" -ForegroundColor Green }
function Write-Warn    { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err     { param($msg) Write-Host "[ERR ]  $msg" -ForegroundColor Red }
function Write-Sep     { Write-Host ("=" * 59) -ForegroundColor DarkGray }

# ── Directorio raíz ───────────────────────────────────────────────────────────
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

# ── Cabecera ──────────────────────────────────────────────────────────────────
Write-Host ""
Write-Sep
Write-Host "  TrastCMS - Compilacion Nativa GraalVM  [Windows]" -ForegroundColor White
Write-Sep
Write-Host ""

# ── Resolver GraalVM (local → GRAALVM_HOME → JAVA_HOME → PATH) ───────────────
Write-Info "Buscando GraalVM..."

$GraalvmBin = $null

# 1. tools\graalvm\ dentro del proyecto (recomendado para equipos)
$LocalGraalvm = Join-Path $Root 'tools\graalvm'
if ((Test-Path (Join-Path $LocalGraalvm 'bin\native-image.cmd')) -or (Test-Path (Join-Path $LocalGraalvm 'bin\native-image.exe'))) {
    $GraalvmBin = Join-Path $LocalGraalvm 'bin'
    Write-Ok "GraalVM local encontrado en tools\graalvm\"

# 2. Variable de entorno GRAALVM_HOME
} elseif ($env:GRAALVM_HOME -and ((Test-Path (Join-Path $env:GRAALVM_HOME 'bin\native-image.cmd')) -or (Test-Path (Join-Path $env:GRAALVM_HOME 'bin\native-image.exe')))) {
    $GraalvmBin = Join-Path $env:GRAALVM_HOME 'bin'
    Write-Ok "GraalVM encontrado via GRAALVM_HOME"

# 3. JAVA_HOME apuntando a GraalVM
} elseif ($env:JAVA_HOME -and ((Test-Path (Join-Path $env:JAVA_HOME 'bin\native-image.cmd')) -or (Test-Path (Join-Path $env:JAVA_HOME 'bin\native-image.exe')))) {
    $GraalvmBin = Join-Path $env:JAVA_HOME 'bin'
    Write-Ok "GraalVM encontrado via JAVA_HOME"

# 4. native-image en el PATH del sistema
} elseif (Get-Command 'native-image' -ErrorAction SilentlyContinue) {
    $GraalvmBin = Split-Path (Get-Command 'native-image').Source
    Write-Ok "GraalVM encontrado en PATH del sistema"

} else {
    Write-Err "GraalVM no encontrado. Opciones:"
    Write-Host ""
    Write-Host "  OPCION A - GraalVM local al proyecto (recomendada):" -ForegroundColor Yellow
    Write-Host "    New-Item -ItemType Directory -Force tools\graalvm"
    Write-Host "    # Descarga GraalVM JDK 25+: https://graalvm.org/downloads/"
    Write-Host "    # Descomprime dentro de $Root\tools\graalvm\"
    Write-Host ""
    Write-Host "  OPCION B - Variable de entorno:" -ForegroundColor Yellow
    Write-Host "    `$env:GRAALVM_HOME = 'C:\ruta\graalvm'"
    Write-Host ""
    Write-Host "  OPCION C - Agregar GraalVM al PATH del sistema:" -ForegroundColor Yellow
    Write-Host "    `$env:PATH = 'C:\ruta\graalvm\bin;' + `$env:PATH"
    exit 1
}

# Agregar GraalVM al PATH de esta sesión
$env:PATH = "$GraalvmBin;$env:PATH"
$env:JAVA_HOME = Split-Path $GraalvmBin

# Advertencia sobre cl.exe (compilador C++ de MSVC)
if (-not (Get-Command 'cl' -ErrorAction SilentlyContinue)) {
    Write-Warn "cl.exe (MSVC) no detectado. Si la compilacion falla, ejecute"
    Write-Warn "este script desde 'x64 Native Tools Command Prompt for VS 2022'."
    Write-Host ""
}

# ── Leer version desde pom.xml ────────────────────────────────────────────────
$Version = (Select-Xml -Path 'pom.xml' -XPath '//*[local-name()="project"]/*[local-name()="version"]' |
    Select-Object -First 1).Node.InnerText.Trim()
$ArtifactName = "trastcms-$Version-windows-x64.exe"

$NativeImageVer = (native-image --version 2>&1 | Select-Object -First 1)
Write-Ok  "GraalVM: $NativeImageVer"
Write-Info "Version      : $Version"
Write-Info "Artefacto    : $ArtifactName"
Write-Info "SkipFrontend : $SkipFrontend"
Write-Info "SkipTests    : $(-not $WithTests)"
Write-Host ""

# ── Variables Maven ───────────────────────────────────────────────────────────
if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xmx512m' }

$MvnArgs = @(
    '--batch-mode',
    '--no-transfer-progress',
    '-Pnative',
    'clean',
    'native:compile'
)
if (-not $WithTests) { $MvnArgs += '-DskipTests' }
if ($SkipFrontend)   { $MvnArgs += '-Dfrontend.skip=true' }

# ── Compilar ──────────────────────────────────────────────────────────────────
Write-Info "Iniciando compilacion nativa (puede tardar 5-15 minutos)..."
Write-Host ""

$StartTime = Get-Date
& '.\mvnw.cmd' @MvnArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Err "La compilacion nativa fallo (exit code $LASTEXITCODE)."
    Write-Err "Revise la salida de Maven arriba para mas detalles."
    exit $LASTEXITCODE
}

$Elapsed = [math]::Round(((Get-Date) - $StartTime).TotalSeconds)
Write-Host ""
Write-Ok "Compilacion completada en ${Elapsed}s"

# ── Empaquetar artefacto ──────────────────────────────────────────────────────
Write-Info "Buscando ejecutable nativo en target\..."

$Executable = Get-ChildItem 'target' -Filter 'trastcms-*.exe' -ErrorAction SilentlyContinue |
    Select-Object -First 1

if (-not $Executable) {
    Write-Err "No se encontro el ejecutable nativo en target\."
    Write-Err "Asegurese de que GraalVM genero el .exe correctamente."
    exit 1
}

New-Item -ItemType Directory -Force -Path 'dist' | Out-Null
New-Item -ItemType Directory -Force -Path 'release' | Out-Null
$Dest = Join-Path 'dist' $ArtifactName
$ReleaseDest = Join-Path 'release' $ArtifactName
Copy-Item $Executable.FullName $Dest -Force
Copy-Item $Executable.FullName $ReleaseDest -Force

# SHA-256 del binario
$Hash = (Get-FileHash $Dest -Algorithm SHA256).Hash.ToLower()
"$Hash  $ArtifactName" | Set-Content "$Dest.sha256" -Encoding ASCII
"$Hash  $ArtifactName" | Set-Content "$ReleaseDest.sha256" -Encoding ASCII

$SizeMB = [math]::Round((Get-Item $Dest).Length / 1MB, 1)

Write-Host ""
Write-Sep
Write-Ok "Ejecutable : $(Resolve-Path $Dest)"
Write-Ok "Checksum   : $(Resolve-Path "$Dest.sha256")"
Write-Host "  Tamano   : ${SizeMB} MB" -ForegroundColor White
Write-Sep
Write-Host ""
Write-Info "Para ejecutar:"
Write-Host "  .\$Dest" -ForegroundColor White
Write-Host ""
