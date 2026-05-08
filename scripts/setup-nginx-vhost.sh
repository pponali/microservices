#!/usr/bin/env bash
# One-time setup on the VM: nginx vhost for apps.khetisahayak.com →
# k3s apigateway NodePort, then Certbot for TLS.
#
# Run on the VM:    sudo bash scripts/setup-nginx-vhost.sh
set -euo pipefail

DOMAIN="${DOMAIN:-apps.khetisahayak.com}"
NODEPORT="${NODEPORT:-30080}"
EMAIL="${EMAIL:-subscribeclaude@gmail.com}"

if [[ $EUID -ne 0 ]]; then
  echo "must run as root (try: sudo bash $0)"; exit 1
fi

# 1. Plain HTTP vhost (Certbot needs port 80 reachable for the ACME challenge).
cat > "/etc/nginx/sites-available/$DOMAIN" <<EOF
server {
    listen 80;
    server_name $DOMAIN;

    # Large request bodies for file uploads through the gateway
    client_max_body_size 50m;

    location / {
        proxy_pass         http://127.0.0.1:$NODEPORT;
        proxy_http_version 1.1;
        proxy_set_header   Host              \$host;
        proxy_set_header   X-Real-IP         \$remote_addr;
        proxy_set_header   X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto \$scheme;
        proxy_set_header   Upgrade           \$http_upgrade;
        proxy_set_header   Connection        "upgrade";
        proxy_read_timeout 300s;
    }
}
EOF

ln -sf "/etc/nginx/sites-available/$DOMAIN" "/etc/nginx/sites-enabled/$DOMAIN"
nginx -t
systemctl reload nginx

# 2. TLS via Certbot (issues a cert AND rewrites the vhost to listen on 443).
if ! command -v certbot >/dev/null 2>&1; then
  apt-get update && apt-get install -y certbot python3-certbot-nginx
fi

certbot --nginx --non-interactive --agree-tos --redirect \
  --email "$EMAIL" -d "$DOMAIN"

nginx -t
systemctl reload nginx

echo "==> $DOMAIN is now fronted by nginx → http://127.0.0.1:$NODEPORT"
echo "==> certbot will auto-renew via the systemd timer 'certbot.timer'"
