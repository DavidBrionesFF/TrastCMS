$ErrorActionPreference = "Stop"

if (-not (Test-Path ".\pom.xml")) {
    Write-Error "Ejecute este script desde la raíz de TrastCMS."
}

$checks = @(
    @{Path=".\pom.xml";Pattern='<version>2\.7\.0-alpha\.9\.5</version>';Message="Versión Maven correcta"},
    @{Path=".\frontend\src\router\index.ts";Pattern='pricing/:productKey/:planKey';Message="Ruta de detalle SaaS instalada"},
    @{Path=".\frontend\src\views\public\CartView.vue";Pattern='checkout-step-layout';Message="Checkout por etapas instalado"},
    @{Path=".\plugins\traststore\frontend\StoreView.vue";Pattern='app-drawer app-drawer--wide';Message="Editor lateral de productos instalado"},
    @{Path=".\plugins\traststore\src\main\java\com\nattechnologies\trastcms\plugins\store\StoreService.java";Pattern='case "REMOVE", "DAMAGE", "SALE", "TRANSFER_OUT"';Message="Las salidas restan inventario"},
    @{Path=".\plugins\trastsaas\frontend\SaasView.vue";Pattern='Promise\.allSettled';Message="SaaS tolera fallos parciales"},
    @{Path=".\plugins\trastcrm\frontend\CrmView.vue";Pattern='deal\.stage\?\.id';Message="Pipeline CRM seguro"},
    @{Path=".\src\main\resources\themes\midnight\tokens.css";Pattern='--surface-muted: #1b2236';Message="Contraste Midnight instalado"},
    @{Path=".\frontend\src\styles.css";Pattern='\.store-admin-card-grid';Message="Cuadrícula Store instalada"}
)

$failed = $false
foreach ($check in $checks) {
    if (-not (Test-Path $check.Path)) {
        Write-Host "[FALTA] $($check.Path)" -ForegroundColor Red
        $failed = $true
    } elseif (Select-String -Path $check.Path -Pattern $check.Pattern -Quiet) {
        Write-Host "[OK] $($check.Message)" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] $($check.Message)" -ForegroundColor Red
        $failed = $true
    }
}

if ($failed) {
    Write-Error "El parche no quedó aplicado correctamente."
}

Write-Host ""
Write-Host "Parche verificado. Ejecute:" -ForegroundColor Cyan
Write-Host ".\mvnw.cmd clean verify" -ForegroundColor Cyan
