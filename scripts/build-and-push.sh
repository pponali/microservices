#!/usr/bin/env bash
# Build every module's Docker image and push to Docker Hub.
# Run this ON THE OCI VM (which is arm64 — images will be native arm64, no buildx needed).
#
# Prereqs:
#   docker login -u pponali     # one-time
#   ./scripts/build-and-push.sh [service-name]   # build one, or all if omitted
set -euo pipefail

REPO="${REPO:-pponali}"
TAG="${TAG:-latest}"

# Modules to build. Order doesn't matter — Kubernetes handles startup ordering.
SERVICES=(
  config-server
  discovery-service
  authorization-service
  resource-server
  oauth2-client
  apigateway
  gateway-service
  user-service
  catalog-service
  product-service
  inventory-service
  order-service
  notification-service
  integration-service
  common-service
  netflix-service
)

build_one() {
  local svc="$1"
  if [[ ! -f "$svc/Dockerfile" ]]; then
    echo ">>> [$svc] no Dockerfile, skipping"
    return 0
  fi
  echo ">>> [$svc] building $REPO/$svc:$TAG"
  docker build -t "$REPO/$svc:$TAG" -f "$svc/Dockerfile" .
  echo ">>> [$svc] pushing"
  docker push "$REPO/$svc:$TAG"
}

if [[ $# -gt 0 ]]; then
  build_one "$1"
else
  for s in "${SERVICES[@]}"; do
    build_one "$s" || echo "!!! [$s] failed (continuing)"
  done
fi
echo "done."
