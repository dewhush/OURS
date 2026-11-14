#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
scp -C "${ROOT_DIR}"/web/* vps:/home/mnemonic/ours/web/
echo "Synced web/ to vps:/home/mnemonic/ours/web/ successfully."
