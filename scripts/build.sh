#!/usr/bin/env sh
set -eu
./mvnw --batch-mode --no-transfer-progress clean package
printf '\nJAR generado en target/trastcms-2.5.0-alpha.7.jar\n'
