#!/usr/bin/env bash
# Deploy the Helm chart to k3s. Run on the OCI VM.
set -euo pipefail

CHART_DIR="$(cd "$(dirname "$0")/.." && pwd)/helm/microservices"
RELEASE="${RELEASE:-microservices}"
NS="${NS:-microservices}"
PHASE="${1:-all}"   # all | infra | platform | apps | ingress

export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"

ensure_helm() {
  command -v helm >/dev/null 2>&1 || {
    echo ">>> installing helm"
    curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
  }
}

ensure_cert_manager() {
  if ! kubectl get ns cert-manager >/dev/null 2>&1; then
    echo ">>> installing cert-manager"
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.3/cert-manager.yaml
    echo ">>> waiting for cert-manager webhook"
    kubectl -n cert-manager wait --for=condition=available --timeout=180s deployment/cert-manager-webhook
  fi
}

ensure_helm

case "$PHASE" in
  infra)
    helm upgrade --install "$RELEASE" "$CHART_DIR" --create-namespace -n "$NS" \
      --set 'services.config-server.enabled=false' \
      --set 'services.discovery-service.enabled=false' \
      --set 'services.authorization-service.enabled=false' \
      --set 'services.resource-server.enabled=false' \
      --set 'services.oauth2-client.enabled=false' \
      --set 'services.apigateway.enabled=false' \
      --set 'services.user-service.enabled=false' \
      --set 'services.catalog-service.enabled=false' \
      --set 'services.product-service.enabled=false' \
      --set 'services.inventory-service.enabled=false' \
      --set 'services.order-service.enabled=false' \
      --set 'services.notification-service.enabled=false' \
      --set 'services.integration-service.enabled=false' \
      --set 'services.common-service.enabled=false' \
      --set ingress.enabled=false
    ;;
  platform)
    ensure_cert_manager
    helm upgrade --install "$RELEASE" "$CHART_DIR" --create-namespace -n "$NS"
    ;;
  all|*)
    ensure_cert_manager
    helm upgrade --install "$RELEASE" "$CHART_DIR" --create-namespace -n "$NS"
    ;;
esac

echo ">>> rollout status"
kubectl -n "$NS" get pods
