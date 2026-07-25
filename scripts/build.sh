#!/usr/bin/env sh
set -eu
./mvnw --batch-mode --no-transfer-progress clean package
printf '\nJAR generado en target/trastcms-2.7.0-alpha.9.5.jar\n'
