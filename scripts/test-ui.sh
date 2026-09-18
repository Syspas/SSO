#!/usr/bin/env bash
# Роль: gate. HtmlUnit UI (@Tag("ui")), H2, приложение в той же JVM.
set -eu
cd "$(dirname "$0")/.."
. ./scripts/ensure-writable-target.sh
./mvnw -B test -Dgroups=ui -DexcludedGroups=architecture,security,smoke \
  -Dsurefire.reportsDirectory=target/surefire-reports-ui "$@"
