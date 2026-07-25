$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

if (-not (Get-Command native-image -ErrorAction SilentlyContinue)) {
    throw 'native-image no está disponible. Instale GraalVM 25+, configure JAVA_HOME y abra una nueva terminal.'
}

& .\mvnw.cmd --batch-mode --no-transfer-progress clean -Pnative -DskipTests native:compile
if ($LASTEXITCODE -ne 0) { throw "La compilación nativa falló con código $LASTEXITCODE" }

New-Item -ItemType Directory -Force -Path dist | Out-Null
$Executable = Get-ChildItem target\trastcms-*.exe | Select-Object -First 1
if (-not $Executable) { throw 'Maven terminó pero no se encontró el ejecutable nativo.' }

$Target = Join-Path dist $Executable.Name
Copy-Item $Executable.FullName $Target -Force
Write-Host "Ejecutable nativo: $Target"
