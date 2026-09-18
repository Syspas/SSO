#!/usr/bin/env bash
# Роль: gate. Юнит- и HTTP-тесты без HtmlUnit, ArchUnit и security.
set -eu
cd "$(dirname "$0")/.."
. ./scripts/ensure-writable-target.sh
./mvnw -B test -DexcludedGroups=architecture,ui,security,smoke "$@"
