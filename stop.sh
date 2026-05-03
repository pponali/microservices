#!/usr/bin/env bash
# Tears down the demo stack.

set -e
cd "$(dirname "$0")"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
elif docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
else
  echo "ERROR: docker-compose / docker compose not found."
  exit 1
fi

echo "==> Stopping and removing containers..."
$COMPOSE down

echo "==> Done. Images are kept (next 'run.sh' will start instantly)."
echo "    To also remove images: docker-compose down --rmi local"
