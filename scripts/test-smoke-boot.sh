#!/usr/bin/env bash
# Поднимает jar на свободном порте, гоняет curl-смоук, гасит процесс.
# Не замена четырёх слоёв Maven. Нужен собранный target/sso.jar (package-dist или mvn package).
set -eu
cd "$(dirname "$0")/.."

if ! [ -f target/sso.jar ]; then
  . ./scripts/ensure-writable-target.sh
  ./mvnw -B package -DskipTests
fi

PORT="${SSO_SMOKE_PORT:-18090}"
BASE_URL="http://127.0.0.1:${PORT}/sso"
LOG="${TMPDIR:-/tmp}/sso-smoke-$$.log"
PID=""

cleanup() {
  if [ -n "${PID}" ] && kill -0 "${PID}" 2>/dev/null; then
    kill "${PID}" 2>/dev/null || true
    wait "${PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

SSO_PORT="${PORT}" java -jar target/sso.jar >"${LOG}" 2>&1 &
PID=$!

for _ in $(seq 1 40); do
  if curl --silent --fail --max-time 2 "${BASE_URL}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "${PID}" 2>/dev/null; then
    echo "SSO не стартовал. Лог:" >&2
    cat "${LOG}" >&2
    exit 1
  fi
  sleep 0.5
done

./scripts/test-smoke.sh "${BASE_URL}"
