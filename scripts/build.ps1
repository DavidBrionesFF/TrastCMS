$ErrorActionPreference = "Stop"
.\mvnw.cmd --batch-mode --no-transfer-progress clean package
Write-Host "`nJAR generado en target/trastcms-2.5.0-alpha.7.jar"
