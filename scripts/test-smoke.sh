#!/usr/bin/env bash
# Смоук HTTP против уже запущенного SSO. Пишет Allure в target/allure-results.
# Использование: ./scripts/test-smoke.sh [BASE_URL]
# Пример: ./scripts/test-smoke.sh http://127.0.0.1:8090/sso
set -eu
cd "$(dirname "$0")/.."

BASE_URL="${1:-http://127.0.0.1:8090/sso}"
BASE_URL="${BASE_URL%/}"
RESULTS_DIR="${ALLURE_RESULTS_DIR:-target/allure-results}"

mkdir -p "${RESULTS_DIR}"

ALLURE_FAILED=0

write_allure_result() {
    local name="$1"
    local status="$2"
    local message="$3"
    local start_ms="$4"
    local stop_ms="$5"
    local uuid
    uuid="$(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')"
    local message_json
    message_json="$(printf '%s' "${message}" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null \
        || printf '"%s"' "$(printf '%s' "${message}" | sed 's/\\/\\\\/g; s/"/\\"/g')")"

    cat > "${RESULTS_DIR}/${uuid}-result.json" <<EOF
{
  "uuid": "${uuid}",
  "historyId": "${name}",
  "fullName": "sso-smoke.${name}",
  "name": "${name}",
  "status": "${status}",
  "stage": "finished",
  "start": ${start_ms},
  "stop": ${stop_ms},
  "statusDetails": {
    "message": ${message_json}
  },
  "labels": [
    {"name": "suite", "value": "sso-smoke"},
    {"name": "framework", "value": "curl"},
    {"name": "tag", "value": "smoke"}
  ]
}
EOF
}

check_http() {
    local name="$1"
    local url="$2"
    local expected_code="$3"
    local body_needle="${4:-}"
    local start_ms stop_ms code body tmp
    start_ms="$(date +%s%3N)"
    tmp="$(mktemp)"
    set +e
    code="$(curl --silent --show-error --max-time 15 --output "${tmp}" --write-out '%{http_code}' --location "${url}")"
    local curl_status=$?
    set -e
    body="$(cat "${tmp}")"
    rm -f "${tmp}"
    stop_ms="$(date +%s%3N)"

    if [ "${curl_status}" -ne 0 ]; then
        write_allure_result "${name}" "broken" "curl exit ${curl_status} for ${url}" "${start_ms}" "${stop_ms}"
        ALLURE_FAILED=1
        echo "FAIL ${name}: curl exit ${curl_status} ${url}"
        return
    fi
    if [ "${code}" != "${expected_code}" ]; then
        write_allure_result "${name}" "failed" "HTTP ${code}, ждали ${expected_code}: ${url}" "${start_ms}" "${stop_ms}"
        ALLURE_FAILED=1
        echo "FAIL ${name}: HTTP ${code} (ждали ${expected_code}) ${url}"
        return
    fi
    if [ -n "${body_needle}" ] && ! printf '%s' "${body}" | grep -q "${body_needle}"; then
        write_allure_result "${name}" "failed" "В теле нет «${body_needle}»: ${url}" "${start_ms}" "${stop_ms}"
        ALLURE_FAILED=1
        echo "FAIL ${name}: нет «${body_needle}» в ${url}"
        return
    fi
    write_allure_result "${name}" "passed" "HTTP ${code} ${url}" "${start_ms}" "${stop_ms}"
    echo "OK ${name}: HTTP ${code} ${url}"
}

echo "Смоук SSO против ${BASE_URL}"
check_http "sso-health" "${BASE_URL}/actuator/health" "200" "UP"
check_http "sso-login" "${BASE_URL}/login" "200" "SSO"
check_http "sso-authorize-anon" "${BASE_URL}/authorize?client_id=webapp&redirect_uri=http://127.0.0.1:8080/login/sso/callback" "200"

if [ "${ALLURE_FAILED}" -ne 0 ]; then
    echo "Смоук SSO красный, Allure-результаты в ${RESULTS_DIR}"
    exit 1
fi
echo "Смоук SSO зелёный, Allure-результаты в ${RESULTS_DIR}"
