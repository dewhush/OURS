#!/usr/bin/env bash
# Deploy web assets + server binary to the VPS and restart ours-server.
# Requires: ssh key auth as mnemonic@<host>; sudo on the server for the binary move + restart.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${OURS_VPS_HOST:-vps}"
BIN="${ROOT_DIR}/server/target/release/ours-server"

echo "==> [1/4] Pushing web assets"
bash "${ROOT_DIR}/deploy/push-web-to-vps.sh"

echo "==> [2/4] Uploading server binary via /tmp"
if [ ! -x "${BIN}" ]; then
  echo "Binary missing/not executable: ${BIN} — run 'cargo build --release' in server/" >&2
  exit 1
fi
scp -C "${BIN}" "${HOST}:/tmp/ours-server.new"

echo "==> [3/4] Swapping binary and restarting ours-server (needs sudo)"
ssh "${HOST}" 'bash -s' <<'REMOTE'
set -euo pipefail
sudo mv /tmp/ours-server.new /opt/ours/ours-server
sudo chown mnemonic:mnemonic /opt/ours/ours-server
sudo chmod 755 /opt/ours/ours-server
sudo systemctl restart ours-server
sleep 1
systemctl --no-pager --lines=0 status ours-server || true
REMOTE

echo "==> [4/4] Verifying health"
for i in 1 2 3 4 5; do
  if curl -fsS "http://${OURS_VPS_LAN:-192.168.100.92}:8080/health" 2>/dev/null; then
    echo; echo "Health OK."
    exit 0
  fi
  sleep 2
done
echo "Health check did not respond yet — check 'sudo systemctl status ours-server' on the server." >&2
exit 1
