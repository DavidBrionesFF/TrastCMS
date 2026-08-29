@echo off
:: =============================================================================
:: build-native.bat — Compila TrastCMS como ejecutable nativo con GraalVM
::
:: IMPORTANTE: Ejecutar desde "x64 Native Tools Command Prompt for VS"
::             GraalVM en Windows requiere cl.exe de Visual Studio.
::
:: Resolucion de GraalVM (en orden de prioridad):
::   1. tools\graalvm\  dentro del proyecto  <- recomendado para equipos
::   2. Variable de entorno GRAALVM_HOME
::   3. Variable de entorno JAVA_HOME (si apunta a GraalVM)
::   4. native-image disponible en PATH del sistema
::
:: Para usar GraalVM local:
::   mkdir tools\graalvm
::   REM Descarga GraalVM JDK 25+ desde https://graalvm.org/downloads/
::   REM Descomprime dentro de tools\graalvm\
::   REM Estructura esperada: tools\graalvm\bin\native-image.exe
::
:: Uso:
::   build-native.bat                   compilacion completa
::   build-native.bat --skip-frontend   omite build de Vue
::   build-native.bat --help
::
:: Requisitos:
::   - GraalVM JDK 25+
::   - Visual Studio Build Tools 2022 con "Desktop development with C++"
::   - Maven Wrapper (mvnw.cmd) en la raiz del proyecto
::   - Memoria recomendada: 8 GB libres
:: =============================================================================
setlocal EnableDelayedExpansion

:: ── Directorio raíz ──────────────────────────────────────────────────────────
set "SCRIPT_DIR=%~dp0"
set "ROOT=%SCRIPT_DIR%.."
pushd "%ROOT%"

:: ── Parsear argumentos ────────────────────────────────────────────────────────
set "SKIP_FRONTEND=false"
set "SHOW_HELP=false"

for %%A in (%*) do (
    if /I "%%A"=="--skip-frontend" set "SKIP_FRONTEND=true"
    if /I "%%A"=="--help"          set "SHOW_HELP=true"
    if /I "%%A"=="-h"              set "SHOW_HELP=true"
)

if "!SHOW_HELP!"=="true" (
    echo Uso: build-native.bat [--skip-frontend] [--help]
    echo.
    echo   --skip-frontend   Omite la compilacion del frontend Vue ^(Vite^)
    echo.
    echo IMPORTANTE: Ejecutar desde "x64 Native Tools Command Prompt for VS"
    popd & exit /b 0
)

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   TrastCMS — Compilacion Nativa GraalVM  [Windows]
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

:: ── Resolver GraalVM (local → GRAALVM_HOME → JAVA_HOME → PATH) ───────────────
echo [INFO]  Buscando GraalVM...

set "GRAALVM_BIN="

:: 1. tools\graalvm\ dentro del proyecto (recomendado para equipos)
if exist "%ROOT%\tools\graalvm\bin\native-image.exe" (
    set "GRAALVM_BIN=%ROOT%\tools\graalvm\bin"
    echo [ OK ]  GraalVM local encontrado en tools\graalvm\
    goto :graalvm_found
)

:: 2. Variable de entorno GRAALVM_HOME
if defined GRAALVM_HOME (
    if exist "%GRAALVM_HOME%\bin\native-image.exe" (
        set "GRAALVM_BIN=%GRAALVM_HOME%\bin"
        echo [ OK ]  GraalVM encontrado via GRAALVM_HOME
        goto :graalvm_found
    )
)

:: 3. JAVA_HOME apuntando a GraalVM
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\native-image.exe" (
        set "GRAALVM_BIN=%JAVA_HOME%\bin"
        echo [ OK ]  GraalVM encontrado via JAVA_HOME
        goto :graalvm_found
    )
)

:: 4. native-image en el PATH del sistema
where native-image >nul 2>&1
if not errorlevel 1 (
    for /f "delims=" %%P in ('where native-image') do set "GRAALVM_BIN=%%~dpP"
    set "GRAALVM_BIN=!GRAALVM_BIN:~0,-1!"
    echo [ OK ]  GraalVM encontrado en PATH del sistema
    goto :graalvm_found
)

:: No encontrado
echo [ERR]  GraalVM no encontrado. Opciones:
echo.
echo   OPCION A - GraalVM local al proyecto ^(recomendada^):
echo     mkdir tools\graalvm
echo     REM Descarga GraalVM JDK 25+: https://graalvm.org/downloads/
echo     REM Descomprime dentro de %ROOT%\tools\graalvm\
echo.
echo   OPCION B - Variable de entorno:
echo     set GRAALVM_HOME=C:\ruta\a\graalvm
echo.
echo   OPCION C - Agregar GraalVM al PATH del sistema:
echo     set PATH=C:\ruta\a\graalvm\bin;%PATH%
popd & exit /b 1

:graalvm_found
:: Agregar GraalVM al PATH de esta sesion
set "PATH=!GRAALVM_BIN!;%PATH%"

:: ── Validar cl.exe (compilador C++ de Visual Studio) ─────────────────────────
where cl >nul 2>&1
if errorlevel 1 (
    echo [WARN] cl.exe ^(compilador C++ de MSVC^) no detectado.
    echo        Si la compilacion falla, abralo desde:
    echo        Inicio ^> "x64 Native Tools Command Prompt for VS 2022"
)

:: ── Leer version desde pom.xml ────────────────────────────────────────────────
for /f "tokens=2 delims=<>" %%V in ('findstr /C:"<version>" pom.xml') do (
    set "VERSION=%%V"
    goto :version_done
)
:version_done
set "ARTIFACT=trastcms-%VERSION%-windows-x64.exe"

echo [INFO]  GraalVM:      & native-image --version 2>&1 | findstr /V "^$"
echo [INFO]  Version:      %VERSION%
echo [INFO]  Artefacto:    %ARTIFACT%
echo [INFO]  Skip frontend: %SKIP_FRONTEND%
echo.

:: ── Construir argumentos Maven ────────────────────────────────────────────────
set "MVN_ARGS=--batch-mode --no-transfer-progress -Pnative -DskipTests clean native:compile"
if "!SKIP_FRONTEND!"=="true" (
    set "MVN_ARGS=!MVN_ARGS! -Dfrontend.skip=true"
)

:: MAVEN_OPTS: limita heap del propio Maven; native-image gestiona el suyo
if not defined MAVEN_OPTS set "MAVEN_OPTS=-Xmx512m"

:: ── Compilar ──────────────────────────────────────────────────────────────────
echo [INFO]  Iniciando compilacion nativa ^(puede tardar 5-15 minutos^)...
echo.

call mvnw.cmd %MVN_ARGS%
if errorlevel 1 (
    echo.
    echo [ERR] La compilacion nativa fallo ^(exit code %errorlevel%^).
    echo       Revise la salida de Maven arriba para mas detalles.
    popd & exit /b 1
)

echo.
echo [ OK ]  Compilacion Maven completada.

:: ── Empaquetar artefacto ──────────────────────────────────────────────────────
echo [INFO]  Buscando ejecutable nativo en target\...

set "EXECUTABLE="
for %%F in (target\trastcms-*.exe) do (
    set "EXECUTABLE=%%F"
)

if "!EXECUTABLE!"=="" (
    echo [ERR] No se encontro el ejecutable nativo en target\.
    echo       Asegurese de que GraalVM genero el .exe correctamente.
    popd & exit /b 1
)

if not exist dist mkdir dist
if not exist release mkdir release
set "DEST=dist\%ARTIFACT%"
set "RELEASE_DEST=release\%ARTIFACT%"
copy /Y "!EXECUTABLE!" "%DEST%" >nul
copy /Y "!EXECUTABLE!" "%RELEASE_DEST%" >nul

:: SHA-256 del binario (requiere PowerShell)
powershell -NoProfile -Command ^
  "(Get-FileHash '%DEST%' -Algorithm SHA256).Hash.ToLower() + '  %ARTIFACT%'" ^
  > "%DEST%.sha256"
copy /Y "%DEST%.sha256" "%RELEASE_DEST%.sha256" >nul

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo [ OK ]  Ejecutable : %CD%\%DEST%
echo [ OK ]  Checksum   : %CD%\%DEST%.sha256
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo [INFO]  Para ejecutar:
echo         %DEST%
echo.

popd
endlocal
exit /b 0
