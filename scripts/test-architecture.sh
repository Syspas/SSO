#!/usr/bin/env bash
# Роль: gate. ArchUnit: швы пакетов.
set -eu
cd "$(dirname "$0")/.."
. ./scripts/ensure-writable-target.sh
./mvnw -B test -Dgroups=architecture -Dsurefire.reportsDirectory=target/surefire-reports-architecture "$@"
