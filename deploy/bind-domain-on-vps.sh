#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:-ours.mnemonic.my.id}"
TUNNEL_NAME="ours-vps"

if [ ! -f "$HOME/.cloudflared/cert.pem" ]; then
  echo "Step 1: Authorize Cloudflare domain (select mnemonic.my.id in browser):"
  /usr/bin/cloudflared tunnel login
fi

if ! /usr/bin/cloudflared tunnel list | grep -q " ${TUNNEL_NAME} "; then
  /usr/bin/cloudflared tunnel create "${TUNNEL_NAME}"
fi

TUNNEL_ID="$(/usr/bin/cloudflared tunnel list | awk -v name="${TUNNEL_NAME}" '$2 == name {print $1; exit}')"
mkdir -p "$HOME/.cloudflared"

cat > "$HOME/.cloudflared/ours-config.yml" <<EOF
tunnel: ${TUNNEL_ID}
credentials-file: ${HOME}/.cloudflared/${TUNNEL_ID}.json
ingress:
  - hostname: ${DOMAIN}
    service: http://127.0.0.1:8787
  - service: http_status:404
EOF

/usr/bin/cloudflared tunnel route dns -f "${TUNNEL_NAME}" "${DOMAIN}"

cat > "$HOME/.config/systemd/user/cloudflared-ours.service" <<EOF
[Unit]
Description=Cloudflare Named Tunnel for ${DOMAIN} (:8787)
After=network-online.target ours-server.service

[Service]
Type=simple
ExecStart=/usr/bin/cloudflared tunnel --no-autoupdate --config ${HOME}/.cloudflared/ours-config.yml run ${TUNNEL_NAME}
Restart=always
RestartSec=5

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reload
systemctl --user restart cloudflared-ours.service
echo "Successfully bound https://${DOMAIN} -> http://127.0.0.1:8787 on VPS!"
