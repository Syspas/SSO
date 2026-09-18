#!/usr/bin/env bash
# Кладёт sso-*.zip на хост через bind-mounts Jenkins.
set -eu
cd "$(dirname "$0")/.."

DEST_HOME="${STAND_ZIP_HOME_DIR:-/output/home-build}"
DEST_PROJECT="${STAND_ZIP_PROJECT_DIR:-/output/project-build}"

if ! [ -d "${DEST_HOME}" ]; then
  echo "Нет ${DEST_HOME}: создайте на хосте /home/master/Build и смонтируйте в compose." >&2
  exit 1
fi

shopt -s nullglob
zips=(target/sso-*.zip)
if [ "${#zips[@]}" -eq 0 ]; then
  echo "Нет target/sso-*.zip: сначала scripts/package-dist.sh." >&2
  exit 1
fi

copied=0
for zip in "${zips[@]}"; do
  name="$(basename "${zip}")"
  install -m 0644 "${zip}" "${DEST_HOME}/${name}"
  if [ -d "${DEST_PROJECT}" ]; then
    install -m 0644 "${zip}" "${DEST_PROJECT}/${name}"
  fi
  copied=$((copied + 1))
  echo "Скопирован ${name} → ${DEST_HOME}"
done
echo "Скопировано архивов: ${copied}"
