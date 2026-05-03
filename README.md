# Microservices: Client-Side & Server-Side Load Balancing

A learning project demonstrating both **client-side** and **server-side** load
balancing in a Spring Boot 3.5.3 / Spring Cloud 2025.0.0 / Java 25 stack, using:

- **Eureka** for service discovery
- **Spring Cloud LoadBalancer** as the actual load-balancing engine
  (Ribbon is gone in Spring Cloud 2023.x)
- **OpenFeign** and **`@LoadBalanced` RestTemplate** for client-side LB
- **Spring Cloud Gateway** with `lb://` URIs for server-side LB

## What is the difference?

| | Who picks the instance? | Who knows the instance list? |
|---|---|---|
| **Client-side LB** | The *caller* service | The *caller* (via Eureka) |
| **Server-side LB** | A middlebox (gateway, Nginx, ELB...) | The middlebox |

In this project the **same** Spring Cloud LoadBalancer powers both — only the
caller is different. catalog-service is a *client-side* caller of user-service.
The gateway is a *server-side* LB sitting in front of user-service for
external clients.

## Architecture

```
                                    ┌────────────────────────┐
                                    │ Eureka :8761           │
                                    │ (discovery-service)    │
                                    └───────────▲────────────┘
                                                │ register / discover
                  ┌─────────────────────────────┼─────────────────────────────┐
                  │                             │                             │
       ┌──────────┴──────────┐    ┌─────────────┴────────────┐    ┌──────────┴──────────┐
       │ api-gateway :8080   │    │ catalog-service :8082    │    │ user-service-1      │
       │ uri: lb://...       │    │ Feign + @LoadBalanced    │    │ user-service-2      │
       │ (server-side LB)    │    │ RestTemplate             │    │ (2 instances :8081) │
       └──────────┬──────────┘    │ (client-side LB)         │    └─────────────────────┘
                  │               └─────────────┬────────────┘
                  ▼                             ▼
           ┌─────────────────────────────────────────────────┐
           │     2 instances of user-service rotate          │
           │     (visible via /users/whoami)                 │
           └─────────────────────────────────────────────────┘
```

## Modules

| Module | Port | Role |
|---|---|---|
| [discovery-service](discovery-service/) | 8761 | Eureka server — every service registers here |
| [user-service](user-service/) | 8081 | The "many instances" service. Run 2x to see LB |
| [catalog-service](catalog-service/) | 8082 | Calls user-service two ways (Feign + RestTemplate) |
| [apigateway](apigateway/) | 8080 | Spring Cloud Gateway, routes via `lb://<service>` |

## Documentation

- **[TESTING.md](TESTING.md)** — every URL, expected output, troubleshooting.
- **[INTERNALS.md](INTERNALS.md)** — how it actually works: the LB algorithm, the cache layers, the URL-rewrite interceptors, the failure-detection timing.

## One-click run

```bash
# macOS / Linux
./run.sh

# Windows
run.bat
```

The script verifies Docker is up, builds all images, starts the stack, waits
for Eureka registration, and prints the test URLs. First run takes a few
minutes (image build + dependency download). Subsequent runs are ~30 s.

Tear down with `./stop.sh` (or `stop.bat`, or `docker-compose down`).

### Manual equivalent

```bash
docker-compose up --build -d
# wait ~60s for Spring Boot apps to register
curl http://localhost:8761/eureka/apps -H 'Accept: application/json'
docker-compose down
```

The first build takes a few minutes (downloads dependencies for each service).
Subsequent builds reuse the Maven cache layer.

Once everything is up, open the **Eureka dashboard** at
[http://localhost:8761](http://localhost:8761). You should see:

- `USER-SERVICE` — **2 instances**
- `CATALOG-SERVICE` — 1 instance
- `API-GATEWAY` — 1 instance

If `USER-SERVICE` only shows 1 instance, give it ~30 seconds — Eureka heartbeats
on a 30-second interval by default.

## Verify load balancing

### 1. Client-side LB via OpenFeign (catalog → user)

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8082/catalog/with-user-feign | jq -c; done
```

The `host` field in the `user` object should **alternate** between the two
user-service container hostnames. catalog-service is picking the instance
itself — that's client-side LB.

### 2. Client-side LB via `@LoadBalanced` RestTemplate

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8082/catalog/with-user-rest | jq -c; done
```

Same alternation. The mechanism is identical (Spring Cloud LoadBalancer);
only the HTTP client is different. Compare `with-user-feign` and
`with-user-rest` source side-by-side to see how `@LoadBalanced` plus the
service-name URL `http://user-service/...` is functionally equivalent to a
`@FeignClient(name = "user-service")`.

### 3. Server-side LB via the gateway (external client → user)

```bash
for i in 1 2 3 4 5 6; do curl -s http://localhost:8080/api/users/whoami | jq -c; done
```

The external client only ever hits `localhost:8080`. The gateway resolves
`lb://user-service` and picks an instance per request — server-side LB.

### 4. Resilience demo (instance death)

```bash
docker-compose stop user-service-1
for i in 1 2 3 4 5 6; do curl -s http://localhost:8080/api/users/whoami | jq -c; done
docker-compose start user-service-1
```

After Eureka deregisters the dead instance (~30–90 s depending on
`eureka.instance.lease-expiration-duration-in-seconds`), all traffic
flows to the surviving replica. This is the payoff of dynamic discovery + LB
over hardcoded URLs.

## Run without Docker

You can also run each service directly with Maven (e.g. for IDE debugging):

```bash
# Terminal 1
cd discovery-service && ./mvnw spring-boot:run     # or: mvn spring-boot:run

# Terminal 2 — first user-service instance on 8081
cd user-service && SERVER_PORT=8081 mvn spring-boot:run

# Terminal 3 — second user-service instance on 8091
cd user-service && SERVER_PORT=8091 mvn spring-boot:run

# Terminal 4
cd catalog-service && mvn spring-boot:run

# Terminal 5
cd apigateway && mvn spring-boot:run
```

All services default to `EUREKA_URL=http://localhost:8761/eureka/` so they
will find the discovery server automatically.

## Key code-level takeaways

- **Service names, never URLs.** Both `lb://user-service` (gateway) and
  `@FeignClient(name = "user-service")` (catalog) refer to the service by name.
  Topology can change freely; code does not.
- **`@LoadBalanced` is the on/off switch** for client-side LB on a RestTemplate
  — see [RestClientConfig.java](catalog-service/src/main/java/com/catalogservice/config/RestClientConfig.java).
- **`lb://` is the on/off switch** for server-side LB in the gateway —
  see [apigateway/.../application.yaml](apigateway/src/main/resources/application.yaml).
- **`prefer-ip-address: true`** is essential under Docker so Eureka registers
  routable container IPs, not Docker-internal hostnames.
- **Unique `instance-id`** (`${spring.application.name}:${random.value}`) is
  what lets two replicas of `user-service` co-exist in Eureka.

## Verified output (2026-05-02)

The full stack was built and run end-to-end with `docker-compose up --build`
on this machine. Below is the **actual** output from the verification curls.

### Eureka registry

```
API-GATEWAY: 1 instance(s)
  - api-gateway:f967fad6282ae724d15eb6f775506701 @ 172.18.0.5:8080  status=UP
CATALOG-SERVICE: 1 instance(s)
  - catalog-service:42fd21b60f79f5c792a0227be9f2ea52 @ 172.18.0.4:8082  status=UP
USER-SERVICE: 2 instance(s)
  - user-service:66d5cb9eec368587d46d49af7e0e5b1d @ 172.18.0.6:8081  status=UP
  - user-service:b101e31bcf7ca7acf7b90b61c21e0435 @ 172.18.0.3:8081  status=UP
```

Two `user-service` instances registered under the same logical name — the
list Spring Cloud LoadBalancer balances across.

### 1. Server-side LB via API Gateway

`for i in 1..6; do curl http://localhost:8080/api/users/whoami; done`

```json
call 1: {"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
call 2: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 3: {"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
call 4: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 5: {"service":"user-service","port":8081,"host":"3a4f78e5f47f"}
call 6: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
```

The external client only ever hits `localhost:8080`, but the gateway alternates
between the two user-service container hostnames per request. Clean
round-robin — that's server-side LB working.

### 2. Client-side LB via OpenFeign

`for i in 1..6; do curl http://localhost:8082/catalog/with-user-feign; done`

```json
call 1: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 2: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
call 3: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 4: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
call 5: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 6: {"calledVia":"OpenFeign + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
```

catalog-service itself is picking the user-service instance — the gateway is
not involved.

### 3. Client-side LB via @LoadBalanced RestTemplate

`for i in 1..6; do curl http://localhost:8082/catalog/with-user-rest; done`

```json
call 1: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 2: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
call 3: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 4: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
call 5: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"a3791ac081bb"}}
call 6: {"calledVia":"@LoadBalanced RestTemplate + Spring Cloud LoadBalancer","user":{"service":"user-service","port":8081,"host":"3a4f78e5f47f"}}
```

Same alternation as Feign — only the HTTP client differs. Same Spring Cloud
LoadBalancer underneath.

### 4. Resilience: kill one user-service instance

```
$ docker stop user-service-1
$ # wait ~30s for Eureka lease to expire
$ curl http://localhost:8761/eureka/apps  → USER-SERVICE: 1 instance only

call 1: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 2: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 3: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 4: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 5: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
call 6: {"service":"user-service","port":8081,"host":"a3791ac081bb"}
```

100% of traffic now flows to the surviving replica. No 5xx, no client-side
config change. This is the payoff of dynamic discovery + LB.

## Next steps (out of scope for this iteration)

- **Resilience4j** circuit breaker around the Feign client.
- **Spring Cloud Config** server (the empty `config-service/` placeholder is
  reserved for this).
- **Distributed tracing** with Micrometer Tracing + Zipkin.
- **Spring Cloud Gateway filters** for rate limiting, request logging, auth.
