$ErrorActionPreference = "Stop"
$Version = "3.9.12"
$Base = Split-Path -Parent $PSScriptRoot
$Home = Join-Path $PSScriptRoot "apache-maven-$Version"
$Maven = Join-Path $Home "bin/mvn.cmd"
if (-not (Test-Path $Maven)) {
    $Archive = Join-Path $PSScriptRoot "apache-maven-$Version-bin.zip"
    $Url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$Version/apache-maven-$Version-bin.zip"
    Write-Host "Maven no está instalado. Descargando Apache Maven $Version..."
    Invoke-WebRequest -Uri $Url -OutFile $Archive
    Expand-Archive -Path $Archive -DestinationPath $PSScriptRoot -Force
    Remove-Item $Archive
}
& $Maven @args
exit $LASTEXITCODE
