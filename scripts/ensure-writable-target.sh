#!/usr/bin/env bash
# Maven пишет в target/. leftover root/nobody после Docker даёт «Отказано в доступе».
# Подключай после cd в корень репозитория:
#   . ./scripts/ensure-writable-target.sh

if ! mkdir -p target/classes 2>/dev/null \
  || ! touch target/classes/.write-test 2>/dev/null; then
  echo "target/ не записывается (часто leftover root/nobody после Docker)." >&2
  echo "Исправь и повтори: sudo rm -rf target  или  sudo chown -R \"\$USER:\$USER\" target" >&2
  exit 1
fi
rm -f target/classes/.write-test
