# Microservices

A Spring Boot 3.5.3 / Spring Cloud 2025.0.0 multi-module learning project that
demonstrates an industry-standard microservices stack: service discovery,
externalized config, API gateway, OAuth 2.0 authorization server, OAuth2
resource servers, event-driven workflows over Kafka, and the cross-cutting
patterns every production REST API needs (RFC 7807 errors, pagination,
versioning, idempotency, rate limiting, caching, webhooks).

> **Why does this project exist?** Most microservices tutorials show one or
> two patterns at a time. This repo wires the patterns together end-to-end
> across 13 modules so you can see how they compose.

---

## Stack

| Component | Version |
|---|---|
| Spring Boot | **3.5.3** |
| Spring Cloud | **2025.0.0** (release train Northfields) |
| Java | source/target **21** (compiled with JDK 25's javac) |
| Maven | 3.9.x |
| Lombok | 1.18.38 |
| springdoc-openapi | 2.6.0 |

External infrastructure used at runtime:

| | What | When required |
|---|---|---|
| **MySQL** | order-service, inventory-service, authorization-service | Always |
| **MongoDB** | product-service | Always |
| **Redis** | gateway sessions, idempotency, product cache | When using gateway / order POSTs / cached reads |
| **Kafka** + **Zookeeper** | OrderPlacedEvent fan-out → notification-service | When testing webhooks |

---

## Architecture

```
                                    ┌──────────────────────────────┐
                                    │  config-server  :8080        │  (externalised config)
                                    │  discovery-service  :8761    │  (Eureka)
                                    └──────────────┬───────────────┘
                                                   │ register / discover
                                                   ▼
   ┌──────────┐    OAuth2 code        ┌──────────────────────────────────────────┐
   │ Browser  │───────────────────────▶ authorization-service  :8080            │
   │ /        │    (issues JWT)        │ Spring Authorization Server, RSA-signed │
   │ oauth2-  │                        └──────────────────────────────────────────┘
   │ client   │                                       │ token introspection
   │ :5555    │                                       ▼
   └────┬─────┘                        ┌──────────────────────────────────────────┐
        │ Bearer JWT                    │ resource-server  :8090                  │
        ▼                               │ @PreAuthorize-protected endpoints       │
   ┌──────────────────────────────┐    └──────────────────────────────────────────┘
   │  gateway-service  :3333      │
   │  Spring Cloud Gateway        │             ┌────────────────────────────┐
   │  • TokenRelay                │────────────▶│ product-service (lb://)    │
   │  • Redis-backed RateLimiter  │             │ MongoDB + Redis cache      │
   │  • Resilience4j CircuitBreak │             └────────────────────────────┘
   │  • Retry                     │             ┌────────────────────────────┐
   │  • SaveSession (Redis)       │────────────▶│ order-service (lb://)      │
   └──────────────┬───────────────┘             │ MySQL + Idempotency-Key    │
                  │                             │ + bulkhead/CB to inventory │
                  │ HTTP                        └─────┬──────────────────────┘
                  │                                   │ WebClient (lb://)
                  │                                   ▼
                  │                             ┌────────────────────────────┐
                  │                             │ inventory-service          │
                  │                             │ MySQL                      │
                  │                             └────────────────────────────┘
                  │
                  │   OrderPlacedEvent          ┌────────────────────────────┐
                  └─────────────Kafka──────────▶│ notification-service       │
                                                │ Kafka consumer             │
                                                │ → outbound webhooks        │
                                                │   (HMAC-SHA256 signed,     │
                                                │    exponential retry)      │
                                                └────────────────────────────┘
```

Cross-cutting concerns live in **common-service** (a library jar) and are
auto-configured into any module that depends on it.

---

## Modules at a glance

| # | Module | Role | Port | Tech |
|---|---|---|---|---|
| 1 | [discovery-service](#1-discovery-service) | Eureka registry — every service finds every other service via this | 8761 | Spring Cloud Netflix Eureka |
| 2 | [config-server](#2-config-server) | Externalised configuration server | 8080 | Spring Cloud Config |
| 3 | [common-service](#3-common-service) | Shared **library** — error handling, pagination, OpenAPI, idempotency | 8080 | Spring Boot library |
| 4 | [gateway-service](#4-gateway-service) | Edge proxy: routing, auth, rate limit, circuit break | 3333 | Spring Cloud Gateway, Redis |
| 5 | [authorization-service](#5-authorization-service) | OAuth 2.0 Authorization Server (issues JWTs) | 8080 | Spring Authorization Server, MySQL |
| 6 | [resource-server](#6-resource-server) | OAuth2-protected resource endpoints | 8090 | Spring Security Resource Server |
| 7 | [oauth2-client](#7-oauth2-client) | Sample browser-flow OAuth2 client | 5555 | Spring Security OAuth2 Client |
| 8 | [product-service](#8-product-service) | Product catalogue with @Cacheable + Redis | dynamic | Spring Data MongoDB, Spring Cache, Redis |
| 9 | [order-service](#9-order-service) | Orders with idempotency + bulkhead/circuit breaker | 8081 | Spring Data JPA (MySQL), Resilience4j, Redis |
| 10 | [inventory-service](#10-inventory-service) | Inventory checks (callee of order-service) | dynamic | Spring Data JPA (MySQL) |
| 11 | [notification-service](#11-notification-service) | Kafka consumer + outbound webhook fan-out | dynamic | Spring Kafka, JPA (H2), RestClient |
| 12 | [integration-service](#12-integration-service) | Kafka + Redis sandbox / data-rest playground | 7070 | Spring Data REST, Kafka, Redis, JPA |
| 13 | [netflix-service](#13-netflix-service) | Spring AI / OpenAI client demo | 8080 | spring-ai-openai-spring-boot-starter |

> "dynamic" port = `server.port=0` → JVM picks a random free port and registers
> it with Eureka. The gateway routes to it via `lb://<service-name>` so callers
> never need to know the actual port.

---

## How the patterns get applied (cross-cutting)

These all live in **common-service** and are auto-applied to any module that adds it as a dependency.

| Pattern | Where | Notes |
|---|---|---|
| **RFC 7807 error handling** | `common-service/.../error/GlobalExceptionHandler.java` | `application/problem+json` for every exception, ~10 specific handlers |
| **Pagination** | `common-service/.../pagination/{PageQuery, PagedResponse}` | Bind `?page=&size=&sort=` and return a stable JSON envelope |
| **OpenAPI** | `common-service/.../openapi/CommonOpenApiAutoConfiguration.java` | Every servlet service auto-exposes `/v3/api-docs` and `/swagger-ui.html` |
| **Idempotency-Key** | `common-service/.../idempotency/` | Stripe-style; opt-in with `services.idempotency.enabled=true`; needs Redis |
| **Rate limiting** | `gateway-service/.../ratelimit/` | Redis token bucket; per-IP and per-user |
| **Throttling (CB / TimeLimiter / Bulkhead / Retry)** | `order-service/...OrderController` | Resilience4j stack on outbound `inventory` calls |
| **Caching** | `product-service/.../config/CacheConfig.java` | `@Cacheable` reads, `@CacheEvict` writes, 10-min Redis TTL |
| **Webhooks** | `notification-service/.../webhook/` | HMAC-SHA256 signed, exponential-backoff retries |

See [API_PATTERNS.md](API_PATTERNS.md) for the deep-dive on each pattern.

---

## Quick start

### Build everything

```bash
mvn -B -DskipTests clean package           # all 13 modules
```

### Run unit + slice tests

```bash
mvn -B test                                # ~21 tests, ~5s, no infra needed
```

### Run integration + context-load tests (requires Docker)

```bash
mvn -B test -Dintegration.tests=true       # adds Testcontainers tests
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the Colima/Docker Desktop setup.

### Build any module's image

Each runnable module has a Dockerfile. Build context is the **repo root** (each
module needs the parent pom + sibling common-service):

```bash
docker build -f product-service/Dockerfile -t product-service:latest .
```

### Run a single service standalone

```bash
mvn -pl <module> spring-boot:run
```

---

## Module deep dives

Each section below covers: **what it does**, **what it depends on**, **how to
run it**, and **what URLs it exposes**.

---

### 1. discovery-service

**Purpose:** Eureka registry. Every other service registers itself here on
startup and queries here to find others. Without this running, nothing else can
find anything.

**Tech:** Spring Cloud Netflix Eureka Server (`@EnableEurekaServer`)
**Port:** `8761`
**Dependencies (runtime):** None — this is the first thing you start.

**Run:**
```bash
mvn -pl discovery-service spring-boot:run
# or
docker build -f discovery-service/Dockerfile -t discovery-service:latest .
docker run -p 8761:8761 discovery-service:latest
```

**Endpoints:**
- `http://localhost:8761` — Eureka dashboard (HTML, shows registered services)
- `http://localhost:8761/eureka/apps` — registry XML/JSON

**Verify it's up:**
```bash
curl http://localhost:8761/eureka/apps -H 'Accept: application/json'
```

---

### 2. config-server

**Purpose:** Centralised configuration server. Other services pull their config
from here at startup so you can tweak settings without rebuilding/redeploying.

**Tech:** Spring Cloud Config (`spring-cloud-config-server`)
**Port:** `8080`
**Dependencies (runtime):** A backing store for configs (default: classpath
or git repo per `application.yml`).

> **Note for this learning project:** other services don't actually pull from
> config-server yet — they use local `application.properties` files. The
> server is wired up but the consumer side is a roadmap item (see
> [ROADMAP.md](ROADMAP.md)).

**Run:**
```bash
mvn -pl config-server spring-boot:run
```

**Endpoints:** `/<application>/<profile>` — fetches config for a named app + profile.

---

### 3. common-service

**Purpose:** Shared library that other modules depend on as a Maven dependency.
Provides project-wide cross-cutting concerns:

- RFC 7807 `ProblemDetail` exception handling (`@ControllerAdvice`)
- `PageQuery` + `PagedResponse<T>` for paginated endpoints
- Auto-configured OpenAPI spec and Swagger UI for every servlet service
- Optional Idempotency-Key filter (Redis-backed)

**Tech:** Spring Boot servlet web + auto-configuration
**Type:** Library jar (the spring-boot-maven-plugin is set `<skip>true</skip>`,
so it produces a normal jar consumable as a Maven dependency, not an executable
fat jar).

**Why it has a main class:** Keeping `@SpringBootApplication` lets us run it
standalone (`mvn spring-boot:run`) for ad-hoc verification, but it isn't
deployed as a service.

**Use it from another module:**
```xml
<dependency>
    <groupId>com.services</groupId>
    <artifactId>common-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

The auto-configurations register themselves — no `@Import` needed.

**Activate idempotency in a consumer service:**
```properties
services.idempotency.enabled=true
spring.data.redis.host=localhost
```

---

### 4. gateway-service

**Purpose:** The single ingress for external clients. Routes `/api/...` to
back-end services discovered via Eureka, applies rate limits, circuit
breakers, retries, OAuth2 token relay, and Redis-backed sessions.

**Tech:** Spring Cloud Gateway (reactive / WebFlux), Resilience4j, Redis
**Port:** `3333`
**Dependencies (runtime):** Eureka (8761), Redis (6379), authorization-service
(for OAuth2 client config).

**What it does in real terms:**

| Route | Target | Filters |
|---|---|---|
| `GET /api/v1/products/**` | `lb://product-service` | RequestRateLimiter (per-IP, 50/s burst 100) |
| `POST/PUT/PATCH/DELETE /api/v1/products/**` | `lb://product-service` | RequestRateLimiter (per-user, 5/s burst 10) |
| `GET /articles/protected` | `http://localhost:8090` | CircuitBreaker, Retry, RequestRateLimiter |

**Run:**
```bash
# 1. start discovery-service first
# 2. start Redis (e.g.: docker run -d -p 6379:6379 redis)
# 3. then:
mvn -pl gateway-service spring-boot:run
```

**Key code:** `gateway-service/src/main/java/com/services/gateway/ratelimit/RateLimiterConfig.java`
provides two `KeyResolver` beans (`ipKeyResolver`, `userKeyResolver`) used by
the gateway's `RequestRateLimiter` filter.

---

### 5. authorization-service

**Purpose:** OAuth 2.0 / OpenID Connect Authorization Server. Issues JWT access
tokens and ID tokens, manages registered OAuth2 clients, and validates user
credentials against a MySQL-backed user store.

**Tech:** Spring Authorization Server, Spring Security, JPA (MySQL)
**Port:** `8080`
**Dependencies (runtime):** MySQL (3306), Eureka.

**What it does:**
- Hosts `/oauth2/authorize`, `/oauth2/token`, `/oauth2/jwks`, `/userinfo`
  endpoints (the Spring Authorization Server defaults).
- RSA-signs JWTs with a key generated at startup (rotate by replacing
  `JWKSource` config in production).
- Stores `Authorization`, `AuthorizationConsent`, `RegisteredClient`, and
  `SecurityUser` entities in JPA.

**Run:**
```bash
# Start MySQL, then:
mvn -pl authorization-service spring-boot:run
```

**Default schema:** Hibernate `ddl-auto=update` creates the schema on first
run. Production should switch to Flyway/Liquibase (see ROADMAP.md).

**Key endpoints:**
- `POST /oauth2/token` — exchange `code` for JWT (auth code grant)
- `GET /oauth2/jwks` — public keys so resource-server can verify tokens

---

### 6. resource-server

**Purpose:** OAuth 2.0 protected resources. Validates incoming JWTs against
authorization-service's JWK set and enforces method-level authorization with
`@PreAuthorize`.

**Tech:** Spring Security Resource Server, Spring Web
**Port:** `8090`
**Dependencies (runtime):** authorization-service (for `/oauth2/jwks`).

**Sample protected endpoint:** `GET /articles/protected` —
`@PreAuthorize("hasAuthority('SCOPE_read')")`. Without a valid JWT bearing
the `read` scope: 401. With it: returns articles.

**Run:**
```bash
mvn -pl resource-server spring-boot:run
```

**Test:**
```bash
TOKEN=$(curl -s -X POST -u api-client:secret \
  http://localhost:8080/oauth2/token \
  -d 'grant_type=client_credentials&scope=read' | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/articles/protected
```

---

### 7. oauth2-client

**Purpose:** Sample browser-flow OAuth2 client. The "test driver" that
exercises the full Authorization Code grant flow against authorization-service.

**Tech:** Spring Security OAuth2 Client, WebFlux
**Port:** `5555`
**Dependencies (runtime):** authorization-service.

> Not registered in the parent `pom.xml` modules list — it's a standalone
> demo client. Build it explicitly: `mvn -pl oauth2-client -am package`
> (or use its Dockerfile).

**Run + use:**
```bash
mvn -pl oauth2-client spring-boot:run
# Open http://localhost:5555 in a browser
# → redirects to authorization-service login
# → login → consent → redirected back with a JWT
```

---

### 8. product-service

**Purpose:** Product catalogue. Demonstrates the full **API patterns** stack
(versioning, pagination, RFC 7807 errors, OpenAPI, validation, full HTTP
methods, caching).

**Tech:** Spring Data MongoDB, Spring Cache, Redis
**Port:** dynamic (registered with Eureka)
**Dependencies (runtime):** MongoDB, Redis, Eureka.

**Endpoints:**
| | |
|---|---|
| `POST /api/v1/products` | Create — 201 + Location header |
| `GET /api/v1/products?page=&size=&sort=` | List paginated |
| `GET /api/v1/products/{id}` | Read (cached) |
| `PUT /api/v1/products/{id}` | Full replace |
| `PATCH /api/v1/products/{id}` | Partial update |
| `DELETE /api/v1/products/{id}` | 204 |
| `GET /v3/api-docs` and `/swagger-ui.html` | OpenAPI |

**Caching contract:**
- `getById` cached for 10 min in Redis
- `create`/`replace`/`patch`/`delete` evict the cache so reads see fresh data

**Run:**
```bash
docker run -d -p 27017:27017 mongo:6
docker run -d -p 6379:6379 redis:7
mvn -pl product-service spring-boot:run
```

---

### 9. order-service

**Purpose:** Order placement. Demonstrates **idempotency** (Stripe-style
`Idempotency-Key`) and **resilience patterns** (Resilience4j Bulkhead +
CircuitBreaker + TimeLimiter + Retry layered on the outbound call to
inventory-service).

**Tech:** Spring Data JPA (MySQL), WebFlux WebClient, Resilience4j, Redis
**Port:** `8081`
**Dependencies (runtime):** MySQL, Redis, Eureka, inventory-service (callee).

**Endpoints:**
| | |
|---|---|
| `POST /api/v1/orders` | Place order. Send `Idempotency-Key: <UUID>` to make it safe to retry. |

**Idempotency:**
- Repeat with same key → returns first response, sets `Idempotent-Replayed: true`
- Same key + different body → 422
- Concurrent same key → 409 (loser retries)
- 24h TTL in Redis

**Resilience stack on `placeOrder`:**
1. `@Bulkhead` — max 20 concurrent calls to inventory
2. `@TimeLimiter` — abort at 3s
3. `@CircuitBreaker` — opens at 50% failure rate (5-call window), 5s cooldown
4. `@Retry` — 3 attempts, 5s wait

---

### 10. inventory-service

**Purpose:** Inventory check service. Called by order-service over HTTP to
verify whether SKUs are in stock before persisting an order.

**Tech:** Spring Data JPA (MySQL)
**Port:** dynamic (registered with Eureka)
**Dependencies (runtime):** MySQL, Eureka.

**Endpoints:**
| | |
|---|---|
| `GET /api/v1/inventory?skuCode=...&skuCode=...` | Returns stock status per SKU |

**Run:** Same as order-service. Note the package typo `com.services.invetory`
(missing 'n') — preserved for backward compatibility.

---

### 11. notification-service

**Purpose:** Two responsibilities:
1. **Kafka consumer** for `OrderPlacedEvent` events from order-service.
2. **Webhook fan-out** — HMAC-SHA256 signed HTTP POSTs to subscribers, with
   exponential-backoff retry (10s, 30s, 2m, 10m, 1h, 6h).

**Tech:** Spring Kafka, JPA (H2 in-memory), RestClient, Spring Web
**Port:** dynamic
**Dependencies (runtime):** Kafka + Zookeeper, Eureka.

**Endpoints:**
| | |
|---|---|
| `POST /api/v1/webhooks` | Register a `(eventType, callbackUrl, secret)` |
| `GET /api/v1/webhooks` | List subscriptions (paginated) |
| `GET /api/v1/webhooks/{id}` | One subscription |
| `DELETE /api/v1/webhooks/{id}` | Unregister |

**Webhook signature format:** Stripe-style header
`X-Webhook-Signature: t=<unix>,v1=<hex_sha256>`. Receivers re-compute
HMAC-SHA256 over `<unix>.<raw_body>` using the shared secret to verify.

**Run:** Needs Kafka. Easiest path:
```bash
docker run -d -p 9092:9092 -p 2181:2181 confluentinc/cp-kafka:latest
mvn -pl notification-service spring-boot:run
```

---

### 12. integration-service

**Purpose:** Sandbox / playground for integration patterns:
- Spring Data REST endpoints (auto-generated CRUD over JPA)
- Kafka producer/consumer demos
- Redis demos

**Tech:** Spring Data REST, Spring Data JPA (MySQL + H2 fallback), Spring
Kafka, Spring Data Redis
**Port:** `7070`
**Dependencies (runtime):** MySQL (or H2), Redis, Kafka — depending on what
you exercise.

**Run:** Standalone:
```bash
mvn -pl integration-service spring-boot:run
```

**Endpoints:** Spring Data REST exposes JPA repositories at `/articles`, etc.,
following HAL+JSON conventions.

---

### 13. netflix-service

**Purpose:** Spring AI demo. Calls OpenAI's chat completions API via
`spring-ai-openai-spring-boot-starter`. Despite the name, it has no relation
to Netflix OSS — it's a leftover module name.

**Tech:** Spring AI OpenAI starter
**Port:** `8080`
**Dependencies (runtime):** OPENAI_API_KEY env var.

**Run:**
```bash
export OPENAI_API_KEY=sk-...
mvn -pl netflix-service spring-boot:run
```

> The Spring AI starter API moves fairly often. If startup fails on a missing
> bean, the issue is most likely a Spring AI version drift — check the
> compatibility matrix at https://docs.spring.io/spring-ai.

---

## Working with the project — local development

### Recommended bring-up order (when running everything)

1. **discovery-service** (8761) — start first, give it 10s
2. **config-server** (8080) — only if you've wired services to consume from it
3. **MySQL** (3306), **MongoDB** (27017), **Redis** (6379), **Kafka** (9092)
4. **authorization-service** (8080)
5. **resource-server** (8090)
6. **product-service**, **order-service**, **inventory-service**, **notification-service** (any order)
7. **gateway-service** (3333) — last; everything else needs to be in Eureka first
8. **oauth2-client** (5555) for browser-driven testing

### Build & run loop (single module)

```bash
mvn -pl product-service -am compile        # compile only
mvn -pl product-service -am test           # run module's tests
mvn -pl product-service spring-boot:run    # hot-reload server
```

### Adding a new endpoint to an existing service

Follow the patterns from `product-service/.../ProductController.java`:
- URL: `/api/v1/<resource>` (plural noun)
- Validation: `@Valid` on `@RequestBody` + Bean Validation annotations on the DTO
- Errors: `throw new ResourceNotFoundException(...)` from common-service —
  let `GlobalExceptionHandler` convert it to RFC 7807
- Pagination: bind `PageQuery`, return `PagedResponse<T>`
- OpenAPI: `@Tag` on controller, `@Operation` + `@ApiResponses` on each method
- Status codes: 200 (read), 201+Location (create), 204 (delete), 404/400/409 via exceptions

### Adding a new module

1. Create the directory + `pom.xml` extending the parent
2. Add it to the parent `pom.xml`'s `<modules>` list
3. Add `common-service` as a dependency for the cross-cutting concerns
4. Add a `Dockerfile` (copy one from a similar existing module)
5. Add `@EnableDiscoveryClient` if it should register with Eureka

---

## Documentation index

| File | When to read it |
|---|---|
| **[README.md](README.md)** *(this file)* | First contact — what is this project, what's in it |
| **[API_PATTERNS.md](API_PATTERNS.md)** | How each of the 20 industry-standard API patterns is implemented + where |
| **[ROADMAP.md](ROADMAP.md)** | What's done, what's coming next, file pointers |
| **[CONTRIBUTING.md](CONTRIBUTING.md)** | Local dev setup (JDK, Maven, Colima, Testcontainers) + commit style |

---

## CI

`.github/workflows/maven.yml` runs on every push/PR to master:
1. `mvn package` — unit + slice tests (no infra needed)
2. `mvn verify -Dintegration.tests=true` — context-load + Testcontainers tests
   (currently `continue-on-error: true` until skeleton context-load tests are
   replaced with real assertions; tracked in ROADMAP.md §7)

---

## Branches

| Branch | What's there |
|---|---|
| **master** | The full commerce-style microservices stack documented above |
| **main** | A separate, smaller learning project: client-side & server-side load balancing demo (4 services, see its README) |

The two branches share **no commit history** — they're independent trees in the same repo.
