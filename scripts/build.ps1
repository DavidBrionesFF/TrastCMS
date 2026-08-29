$ErrorActionPreference = "Stop"

# Directorio raíz
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root

# Configurar JAVA_HOME a GraalVM JDK 25 local si existe
$LocalGraalvm = Join-Path $Root 'tools\graalvm'
if (Test-Path (Join-Path $LocalGraalvm 'bin\java.exe')) {
    $env:JAVA_HOME = (Resolve-Path $LocalGraalvm).Path
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "[INFO] Usando GraalVM JDK local desde tools\graalvm" -ForegroundColor Cyan
}

.\mvnw.cmd --batch-mode --no-transfer-progress clean package -DskipTests

New-Item -ItemType Directory -Force -Path "release" | Out-Null
New-Item -ItemType Directory -Force -Path "dist" | Out-Null

$JarFile = Get-ChildItem "target/trastcms-*.jar" | Select-Object -First 1
if ($JarFile) {
    Copy-Item $JarFile.FullName "release/$($JarFile.Name)" -Force
    Copy-Item $JarFile.FullName "dist/$($JarFile.Name)" -Force
    Write-Host "`n[OK] JAR generado y copiado exitosamente a release/$($JarFile.Name)" -ForegroundColor Green
} else {
    Write-Host "`nJAR generado en target/" -ForegroundColor Yellow
}
