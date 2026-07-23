#!/usr/bin/env sh
set -eu
./mvnw --batch-mode --no-transfer-progress clean package
printf '\nJAR generado en target/trastcms-2.6.0-alpha.8.jar\n'
