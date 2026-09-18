#!/usr/bin/env bash
# Роль: gate. Поведенческие тесты безопасности (@Tag("security")).
set -eu
cd "$(dirname "$0")/.."
. ./scripts/ensure-writable-target.sh
./mvnw -B test -Dgroups=security -DexcludedGroups=architecture,ui,smoke \
  -Dsurefire.reportsDirectory=target/surefire-reports-security "$@"
