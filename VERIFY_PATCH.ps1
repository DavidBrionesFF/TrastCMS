$ErrorActionPreference = "Stop"

if (-not (Test-Path ".\pom.xml")) {
    Write-Error "Ejecute este script desde la raiz del proyecto TrastCMS."
}

$checks = @(
    @{
        Path = ".\src\test\java\com\nattechnologies\trastcms\StarterContentAndCrmIntegrationTests.java"
        Pattern = 'jsonPath\("\$\.themeOrigin"\)\.value\("nexus"\)'
        Description = "El test espera el tema Nexus"
    },
    @{
        Path = ".\plugins\trastpay\src\main\java\com\nattechnologies\trastcms\plugins\pay\PayService.java"
        Pattern = 'created\.id = UUID\.randomUUID\(\)\.toString\(\);'
        Description = "Las lineas nuevas del carrito reciben UUID antes del calculo"
    },
    @{
        Path = ".\plugins\trastcrm\src\main\java\com\nattechnologies\trastcms\plugins\crm\CrmService.java"
        Pattern = 'import java\.time\.Duration;'
        Description = "CrmService importa Duration"
    },
    @{
        Path = ".\plugins\trastcrm\src\main\java\com\nattechnologies\trastcms\plugins\crm\CrmService.java"
        Pattern = 'import java\.time\.LocalDate;'
        Description = "CrmService importa LocalDate"
    }
)

$failed = $false

foreach ($check in $checks) {
    if (-not (Test-Path $check.Path)) {
        Write-Host "[FALTA] $($check.Path)" -ForegroundColor Red
        $failed = $true
        continue
    }

    $match = Select-String `
        -Path $check.Path `
        -Pattern $check.Pattern `
        -Quiet

    if ($match) {
        Write-Host "[OK] $($check.Description)" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $($check.Description)" -ForegroundColor Red
        $failed = $true
    }
}

$nestedPatchFolders = Get-ChildItem `
    -Directory `
    -Filter "TrastCMS-*-hotfix*" `
    -ErrorAction SilentlyContinue

if ($nestedPatchFolders) {
    Write-Host ""
    Write-Host "Se detectaron carpetas de parches dentro de la raiz:" -ForegroundColor Yellow
    $nestedPatchFolders | ForEach-Object {
        Write-Host "  - $($_.FullName)" -ForegroundColor Yellow
    }
    Write-Host "Esas carpetas no son compiladas por Maven." -ForegroundColor Yellow
}

if ($failed) {
    Write-Error "El parche no esta aplicado correctamente."
}

Write-Host ""
Write-Host "Parche verificado. Ejecute:" -ForegroundColor Cyan
Write-Host ".\mvnw.cmd clean verify" -ForegroundColor Cyan
