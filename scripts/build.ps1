$ErrorActionPreference = "Stop"
.\mvnw.cmd --batch-mode --no-transfer-progress clean package
Write-Host "`nJAR generado en target/trastcms-2.7.0-alpha.9.5.jar"
