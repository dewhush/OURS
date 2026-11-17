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

echo "==> [3/4] Swapping binary and restarting ours-server (needs sudo for port <1024 only if bound low; ours runs as user on 8787)"
ssh "${HOST}" 'bash -s' <<'REMOTE'
set -euo pipefail
BIN_DIR="${OURS_BIN_DIR:-/home/mnemonic/ours/bin}"
SVC_USER="${OURS_SVC_USER:-mnemonic}"
mkdir -p "$BIN_DIR"
mv /tmp/ours-server.new "$BIN_DIR/ours-server.new"
# Move into place (best-effort sudo; the live layout is user-owned, no sudo needed)
if mv "$BIN_DIR/ours-server.new" "$BIN_DIR/ours-server" 2>/dev/null; then
  :
else
  sudo mv "$BIN_DIR/ours-server.new" "$BIN_DIR/ours-server"
fi
chown "$SVC_USER:$SVC_USER" "$BIN_DIR/ours-server" 2>/dev/null || sudo chown "$SVC_USER:$SVC_USER" "$BIN_DIR/ours-server"
chmod 755 "$BIN_DIR/ours-server"
systemctl --user restart ours-server 2>/dev/null || sudo systemctl restart ours-server
sleep 1
systemctl --user --no-pager --lines=0 status ours-server 2>/dev/null || systemctl --no-pager --lines=0 status ours-server || true
REMOTE

echo "==> [4/4] Verifying health"
HEALTH_HOST="${OURS_VPS_LAN:-192.168.100.92}"
HEALTH_PORT="${OURS_VPS_PORT:-8787}"
for i in 1 2 3 4 5; do
  if curl -fsS "http://${HEALTH_HOST}:${HEALTH_PORT}/health" 2>/dev/null; then
    echo; echo "Health OK."
    exit 0
  fi
  sleep 2
done
echo "Health check did not respond yet — check 'sudo systemctl status ours-server' on the server." >&2
exit 1
