# Testing Guide

Every URL, expected output, and verification step for the load-balancing
demo. Pair with [INTERNALS.md](INTERNALS.md) for the *why* behind each
result.

---

## Prerequisites

You need installed and running:

| Tool | How to get it (macOS) | How to get it (Linux) | How to get it (Windows) |
|---|---|---|---|
| **Docker daemon** | Docker Desktop *or* `brew install colima docker docker-compose` then `colima start` | Docker Engine via your package manager | Docker Desktop (with WSL2) |
| **`docker-compose`** | comes with Docker Desktop, or `brew install docker-compose` | `apt install docker-compose-plugin` | comes with Docker Desktop |
| **`curl`** | preinstalled | preinstalled | preinstalled on Win11, or use PowerShell `Invoke-RestMethod` |
| **`python3`** *(optional)* | preinstalled | usually preinstalled | from python.org or Microsoft Store |

You do **NOT** need a JDK or Maven on your host machine — everything is
compiled inside Docker. (You only need them if you want to run services
without Docker; see the bottom of [README.md](README.md).)

### Verify your setup

```bash
docker --version           # should print Docker version 20.x+
docker-compose --version   # 2.x+ or "Docker Compose version v2.x"
docker info                # should NOT print "Cannot connect to the Docker daemon"
```

If `docker info` errors, your daemon isn't running. On macOS with Colima:
```bash
colima start
```

---

## Single-click run

From the project root (where `docker-compose.yml` lives):

```bash
./run.sh        # macOS / Linux
run.bat         # Windows
```

The script will:
1. Verify Docker is running (and try to start Colima if available).
2. Build all 4 images (~5–10 min on first run, ~30 s on subsequent).
3. Start the 5 containers.
4. Wait until Eureka has all instances registered.
5. Print the URLs you can hit.

When you're done:
```bash
./stop.sh       # macOS / Linux
stop.bat        # Windows
# or directly:
docker-compose down
```

---

## Manual run (if scripts don't work for any reason)

```bash
docker-compose up --build -d         # build and start in background
# wait ~60 seconds for Spring Boot apps to register
curl http://localhost:8761/eureka/apps -H 'Accept: application/json'
# tear down
docker-compose down
```

---

## What you should see — running stack

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

```
NAMES             STATUS         PORTS
discovery         Up 2 minutes   0.0.0.0:8761->8761/tcp
user-service-1    Up 2 minutes
api-gateway       Up 2 minutes   0.0.0.0:8080->8080/tcp
catalog-service   Up 2 minutes   0.0.0.0:8082->8082/tcp
user-service-2    Up 2 minutes
```

Note `user-service-1` and `user-service-2` have **no published port** —
they're only reachable from inside the Docker network. That's deliberate.

---

## Test 0 — Eureka dashboard

| What | URL | Expected |
|---|---|---|
| Eureka UI | http://localhost:8761 | Registry table with 4 instances |
| Eureka registry JSON | http://localhost:8761/eureka/apps (header `Accept: application/json`) | JSON listing 4 instances across 3 services |

**What you're looking for in the UI:**
- `USER-SERVICE` shows **2** instances (different `instanceId`s, both `UP`)
- `CATALOG-SERVICE` shows 1 instance
- `API-GATEWAY` shows 1 instance

If you see no instances, give it 30 seconds — the first heartbeat is on a
delay. If you still don't, see [Troubleshooting](#troubleshooting) below.

---

## Test 1 — Server-side load balancing (gateway)

This is the demo most people associate with "load balancer." External
caller hits **one** address; the gateway internally rotates between
`user-service` instances.

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8080/api/users/whoami; echo; done
```

**Expected output** (the two random hex strings will differ on your machine):

```json
{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
```

**The proof:**
- The `host` value alternates between **two different** Docker container IDs.
- The URL never changed — `localhost:8080/api/users/whoami` always.
- The gateway used the `lb://user-service` URI to resolve *which* instance
  to forward to. That's server-side LB.

**To test in a browser instead:** open http://localhost:8080/api/users/whoami
and hit refresh repeatedly. Same thing.

---

## Test 2 — Client-side load balancing via OpenFeign

Now the LB happens *inside* `catalog-service`. The gateway is not involved.

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8082/catalog/with-user-feign; echo; done
```

**Expected output:**

```json
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
{"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
```

**The proof:**
- Same alternation in the `user.host` field.
- This call went to `localhost:8082` (catalog-service direct, not the gateway).
- catalog-service itself decided which `user-service` instance to call.

**Browser:** http://localhost:8082/catalog/with-user-feign — refresh repeatedly.

---

## Test 3 — Client-side load balancing via `@LoadBalanced` RestTemplate

Same client-side LB, different HTTP-client style.

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8082/catalog/with-user-rest; echo; done
```

**Expected output:**

```json
{"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
{"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
... (alternating)
```

**The proof:**
- Same alternation pattern as Test 2 — because the underlying LB is the same.
- Different code path inside catalog-service: classic `RestTemplate` with the
  `@LoadBalanced` qualifier instead of a Feign interface.

**Browser:** http://localhost:8082/catalog/with-user-rest

---

## Test 4 — Resilience: kill an instance

The dramatic test. We take down one `user-service` and prove that traffic
keeps flowing.

```bash
# 1. Kill one replica
docker stop user-service-1

# 2. Wait ~30 seconds (Eureka deregisters the dead instance after lease expiry)
sleep 30

# 3. Confirm registry now shows only 1 user-service instance
curl -s http://localhost:8761/eureka/apps -H 'Accept: application/json' \
  | python3 -c "import sys,json;d=json.load(sys.stdin);[print(a['name'],'-',(len(a['instance']) if isinstance(a['instance'],list) else 1),'instance(s)') for a in d['applications']['application']]"

# 4. Hammer the gateway 6 times — every call should hit the SURVIVOR only
for i in 1 2 3 4 5 6; do curl -s http://localhost:8080/api/users/whoami; echo; done

# 5. Restart the dead instance — within ~60s rotation resumes
docker start user-service-1
```

**Expected output of step 4:**

```json
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
{"service":"user-service","port":8081,"host":"a3791ac081bb"}
```

All 6 calls to the **same** host — because Eureka has only one instance to
hand out, and the LB rotates within whatever list it has.

**Zero 5xx errors. Zero config changes. The application self-heals.** That's the
payoff of dynamic discovery + load balancing over hardcoded URLs.

> **Edge case:** if you skip the 30s wait and curl immediately after the
> stop, you may see ~50% timeouts because catalog-service still has the dead
> instance in its cached list. See [INTERNALS.md §7](INTERNALS.md#7-failure-detection--resilience)
> for the eviction timeline and how to enable LB-native retry to mask this gap.

---

## Reference: all URLs

| URL | What it tests | Browser-friendly? |
|---|---|---|
| http://localhost:8761 | Eureka dashboard | ✅ |
| http://localhost:8761/eureka/apps | Registry XML/JSON | ✅ |
| http://localhost:8080/api/users/whoami | **Server-side LB** via gateway | ✅ refresh repeatedly |
| http://localhost:8080/api/users | (returns string greeting) | ✅ |
| http://localhost:8080/api/catalog | catalog through gateway | ✅ |
| http://localhost:8082/catalog | catalog direct (no gateway) | ✅ |
| http://localhost:8082/catalog/with-user-feign | **Client-side LB** via Feign | ✅ refresh repeatedly |
| http://localhost:8082/catalog/with-user-rest | **Client-side LB** via RestTemplate | ✅ refresh repeatedly |
| http://localhost:8080/actuator/health | Gateway health | ✅ |
| http://localhost:8082/actuator/health | catalog-service health | ✅ |
| http://localhost:8761/actuator/health | discovery-service health | ✅ |

---

## Live debugging

```bash
docker ps                              # all 5 containers status
docker logs api-gateway -f             # see route resolutions
docker logs catalog-service -f         # see Feign / RestTemplate calls
docker logs user-service-1 -f          # one user-service's traffic
docker logs discovery -f               # Eureka registration events
```

To get more verbose LB logs, edit `catalog-service/src/main/resources/application.yaml`:

```yaml
logging:
  level:
    org.springframework.cloud.loadbalancer: DEBUG
    org.springframework.cloud.openfeign: DEBUG
```

Rebuild: `docker-compose up --build -d catalog-service`. Now `docker logs
catalog-service -f` will show every instance pick.

---

## Troubleshooting

### "0 instances" in the Eureka dashboard

- **Wait 60 seconds** after `docker-compose up`. The first registration is
  delayed.
- Check `docker ps` — all 5 containers should show `Up`.
- `docker logs user-service-1 | grep -i eureka` should show
  `DiscoveryClient_USER-SERVICE/...: registering service...`.

### `Connection refused` from curl

- Make sure the stack is up: `docker ps`.
- Check the host port mapping: `0.0.0.0:8080->8080/tcp` for gateway, etc.
- Mac firewall sometimes blocks Docker — disable temporarily to test.

### Curls all return the same host (no rotation)

- Check `USER-SERVICE` shows 2 instances on http://localhost:8761.
- If only 1, one container failed to start: `docker logs user-service-2`.
- If 2 instances but no rotation: clear caches by waiting 60s and retry —
  the L2 cache TTL (~35s) plus client-side stale-instance handling can
  briefly stick to one instance after restart.

### Build fails with "package org.springframework... does not exist"

- Maven inside Docker couldn't reach Maven Central. Check your network/VPN.
- Re-run with cache disabled: `docker-compose build --no-cache && docker-compose up -d`.

### `docker-compose up` hangs forever

- Ctrl+C, then `docker-compose down -v`, then retry.
- If on macOS with Docker Desktop and the VM seems stuck, restart Docker
  Desktop or `colima restart`.

### "Self preservation mode" warning on Eureka dashboard

- Expected in dev with only 2 user-service instances when one dies.
- See [INTERNALS.md §7](INTERNALS.md#eureka-self-preservation-a-footgun) for
  the explanation and how to disable.

---

## What "passing" looks like

You can claim the demo works end-to-end when **all** of these are true:

- [x] http://localhost:8761 shows USER-SERVICE with 2 instances UP.
- [x] Test 1 produces alternating `host` values across 6 calls.
- [x] Test 2 produces alternating `host` values across 6 calls.
- [x] Test 3 produces alternating `host` values across 6 calls.
- [x] Test 4 produces 6 identical `host` values after stopping one instance,
      with zero curl failures.

When all check, you've demonstrated:
- **service discovery** (Eureka),
- **client-side LB** (catalog-service via Feign + RestTemplate),
- **server-side LB** (gateway via `lb://`),
- **resilience** (instance death tolerated without code changes).
