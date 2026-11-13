#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${OURS_DATA_DIR:-/var/lib/ours}"
BACKUP_ROOT="${OURS_BACKUP_DIR:-/var/backups/ours}"
KEEP_DAYS="${OURS_BACKUP_KEEP_DAYS:-14}"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
DEST="${BACKUP_ROOT}/${STAMP}"

mkdir -p "${DEST}"

if [[ -f "${DATA_DIR}/ours.db" ]]; then
  sqlite3 "${DATA_DIR}/ours.db" ".backup '${DEST}/ours.db'"
fi

if [[ -f "${DATA_DIR}/token_secret" ]]; then
  cp -a "${DATA_DIR}/token_secret" "${DEST}/token_secret"
fi

if [[ -d "${DATA_DIR}/photos" ]]; then
  cp -a "${DATA_DIR}/photos" "${DEST}/photos"
fi

# Prune old backups
find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime "+${KEEP_DAYS}" -exec rm -rf {} +

echo "Backup completed: ${DEST}"
