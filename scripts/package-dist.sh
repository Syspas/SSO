#!/usr/bin/env bash
# Zip дистрибутива из того же кода, который только что прошли тесты.
# -DskipTests не обход проверки: слои уже отработали в этой же папке.
set -eu
cd "$(dirname "$0")/.."
. ./scripts/ensure-writable-target.sh
./mvnw -B package -Pdist -DskipTests "$@"
