<#
==============================================================================
 Script de Desarrollo Frontend en Vivo (Vite Dev Server + Proxy a Spring Boot)
==============================================================================
 Uso:
   .\dev.ps1
   .\dev.ps1 -ApiUrl "http://localhost:8080"
   .\dev.ps1 -ApiUrl "http://localhost:8080" -Port 5173
==============================================================================
#>
param (
    [string]$ApiUrl = "",
    [int]$Port = 0
)

# Cargar .env si existe
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), [System.EnvironmentVariableTarget]::Process)
        }
    }
}

if (-not $ApiUrl) {
    $ApiUrl = [System.Environment]::GetEnvironmentVariable("VITE_API_URL")
    if (-not $ApiUrl) { $ApiUrl = "http://localhost:8080" }
}

if ($Port -eq 0) {
    $envPort = [System.Environment]::GetEnvironmentVariable("VITE_PORT")
    if ($envPort) { $Port = [int]$envPort } else { $Port = 5173 }
}

$env:VITE_API_URL = $ApiUrl
$env:VITE_PORT = $Port

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " 🚀 Iniciando Servidor de Desarrollo Frontend (Vite)" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " 🌐 Servidor Frontend:  http://localhost:$Port" -ForegroundColor Green
Write-Host " 🔗 Backend API Target: $ApiUrl" -ForegroundColor Yellow
Write-Host " ⚡ Cambios en vivo (HMR) habilitados sin recompilar Spring Boot." -ForegroundColor Green
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location -Path "frontend"
npm run dev
