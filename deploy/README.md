# Deploying OURS Server

Target host: `192.168.100.92` on the LAN, SSH user `mnemonic` (key auth).

## 1. Build the release binary

On your workstation:

```bash
cd server
cargo build --release --offline
```

The stripped binary is at `server/target/release/ours-server`.

## 2. Copy binary and service files to the server

```bash
ssh mnemonic@192.168.100.92 "sudo mkdir -p /opt/ours /etc/ours /var/lib/ours /var/backups/ours && sudo chown -R mnemonic:mnemonic /opt/ours /var/lib/ours /var/backups/ours"
scp server/target/release/ours-server mnemonic@192.168.100.92:/opt/ours/ours-server
scp deploy/backup.sh mnemonic@192.168.100.92:/opt/ours/backup.sh
scp deploy/ours.env.example mnemonic@192.168.100.92:/tmp/ours.env
scp deploy/ours-server.service mnemonic@192.168.100.92:/tmp/ours-server.service
```

On the server (`ssh mnemonic@192.168.100.92`):

```bash
chmod +x /opt/ours/ours-server /opt/ours/backup.sh
sudo mv /tmp/ours.env /etc/ours/ours.env
sudo chmod 600 /etc/ours/ours.env
# Edit /etc/ours/ours.env and set OURS_SETUP_KEY
sudo mv /tmp/ours-server.service /etc/systemd/system/ours-server.service
sudo systemctl daemon-reload
sudo systemctl enable --now ours-server
```

Verify locally on the LAN:

```bash
curl http://192.168.100.92:8080/health
```

## 3. Cloudflare Tunnel (public HTTPS + WebSocket)

Install `cloudflared` on the server, authenticate, create a tunnel, and configure `/etc/cloudflared/config.yml` based on `deploy/cloudflared.yml.example`. Enable `cloudflared` as a systemd service (`sudo cloudflared service install`).

## 4. Backups

Ensure `sqlite3` is installed (`sudo apt install -y sqlite3`), then add a daily cron job for `mnemonic`:

```cron
15 3 * * * /opt/ours/backup.sh >> /var/log/ours-backup.log 2>&1
```
