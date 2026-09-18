#!/usr/bin/env bash
# Роль: gate (локальная сдача одним заходом).
# Слои: unit → security → ui → architecture. Смоук — отдельный scripts/test-smoke-boot.sh.
# Jenkins эту обёртку не зовёт. Не делай mvn clean между слоями.
set -eu
cd "$(dirname "$0")/.."

echo "1/4 Юнит-тесты"
./scripts/test-unit.sh

echo "2/4 Безопасность"
./scripts/test-security.sh

echo "3/4 UI (HtmlUnit)"
./scripts/test-ui.sh

echo "4/4 Архитектура"
./scripts/test-architecture.sh

echo "Четыре слоя зелёные. Отчёт: ./mvnw allure:serve"
