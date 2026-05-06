#!/usr/bin/env bash
# One-click runner for the microservices LB demo.
# Builds all images, starts the stack, waits for Eureka, prints test URLs.

set -e

cd "$(dirname "$0")"

# ---- 1. preflight ----
need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: '$1' is not on PATH. Install it first."
    echo "       (See TESTING.md → Prerequisites)"
    exit 1
  fi
}
need docker

if ! command -v docker-compose >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1; then
    COMPOSE="docker compose"
  else
    echo "ERROR: neither 'docker-compose' nor 'docker compose' is available."
    exit 1
  fi
else
  COMPOSE="docker-compose"
fi

# ---- 2. ensure docker daemon is up ----
if ! docker info >/dev/null 2>&1; then
  if command -v colima >/dev/null 2>&1; then
    echo "Docker daemon not running — starting Colima..."
    colima start
  else
    echo "ERROR: Docker daemon is not running."
    echo "       Start Docker Desktop (or 'colima start') and re-run this script."
    exit 1
  fi
fi

echo "==> Building images and starting the stack (first run takes a few minutes)..."
$COMPOSE up --build -d

# ---- 3. wait for Eureka ----
echo "==> Waiting for Eureka server to come up..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8761/ >/dev/null 2>&1; then
    echo "    Eureka HTTP is up."
    break
  fi
  sleep 2
done

# ---- 4. wait for all 4 client instances to register ----
echo "==> Waiting for 2x user-service + catalog-service + api-gateway to register (~30-60s)..."
for i in $(seq 1 60); do
  COUNT=$(curl -s http://localhost:8761/eureka/apps -H 'Accept: application/json' 2>/dev/null \
    | python3 -c "import sys,json
try:
  d=json.load(sys.stdin)
  apps=d.get('applications',{}).get('application',[])
  if isinstance(apps,dict): apps=[apps]
  total=0
  for a in apps:
    inst=a.get('instance',[])
    if isinstance(inst,dict): inst=[inst]
    total+=len(inst)
  print(total)
except: print(0)
" 2>/dev/null || echo 0)
  if [ "$COUNT" = "4" ]; then
    echo "    All 4 instances registered."
    break
  fi
  echo "    ($i/60) registered so far: $COUNT — waiting..."
  sleep 2
done

# ---- 5. print summary ----
cat <<EOF

============================================================
  STACK IS READY
============================================================

  Eureka dashboard      http://localhost:8761
  Server-side LB demo   http://localhost:8080/api/users/whoami
  Feign client-side LB  http://localhost:8082/catalog/with-user-feign
  RestTemplate LB       http://localhost:8082/catalog/with-user-rest

  Hit those URLs MULTIPLE times in a row — the 'host' field
  in the JSON should rotate between the two user-service
  containers. That's the load balancing in action.

  Quick test:
     for i in 1 2 3 4 5 6; do curl -s http://localhost:8080/api/users/whoami; echo; done

  See TESTING.md for the full test plan and INTERNALS.md
  for how everything works under the hood.

  Tear down with:
     ./stop.sh
     (or:  docker-compose down  )

============================================================
EOF
