# API Patterns Reference

This document maps the **20 API topics** to their implementation in this codebase
and explains the *why* behind each pattern.

It pairs with [ROADMAP.md](ROADMAP.md), which lists the remaining items not yet
implemented and how to add them.

---

## Stack

- **Spring Boot** 3.5.3
- **Spring Cloud** 2025.0.0 (release train "Northfields")
- **Java** 21 (compiled by JDK 25's javac, target bytecode 21 — see parent `pom.xml`)
- **Lombok** 1.18.38
- **springdoc-openapi** 2.6.0

---

## The 20 topics, mapped

| # | Topic | Status | Where it lives |
|---|---|---|---|
| 1 | Endpoint | ✅ FULL | Every `@RestController` across the 12 modules |
| 2 | HTTP Methods | ✅ FULL | [ProductController](product-service/src/main/java/com/services/product/controller/ProductController.java) demonstrates GET / POST / PUT / PATCH / DELETE |
| 3 | Request-Response | ✅ FULL | [ProductRequest](product-service/src/main/java/com/services/product/dto/ProductRequest.java) (with validation) + [ProductResponse](product-service/src/main/java/com/services/product/dto/ProductResponse.java) + [ProductPatchRequest](product-service/src/main/java/com/services/product/dto/ProductPatchRequest.java) |
| 4 | Status Codes | ✅ FULL | 200 / 201 + `Location` / 204 / 400 / 403 / 404 / 405 / 409 / 500 — all flowing through [GlobalExceptionHandler](common-service/src/main/java/com/services/common/error/GlobalExceptionHandler.java) |
| 5 | Authentication | ✅ FULL | OAuth2 authorization-server in [authorization-service](authorization-service/) issues JWTs |
| 6 | Authorization | ✅ FULL | `@PreAuthorize` in [resource-server](resource-server/) endpoints |
| 7 | Access Tokens | ✅ FULL | RSA-signed JWTs minted by authorization-service, validated by resource-server |
| 8 | OAuth 2.0 | ✅ FULL | authorization-service (server) + resource-server (RS) + gateway-service (client w/ TokenRelay) + oauth2-client (client app) |
| 9 | Rate Limiting | ✅ NEW | [gateway-service application.yaml](gateway-service/src/main/resources/application.yaml) + [RateLimiterConfig](gateway-service/src/main/java/com/services/gateway/ratelimit/RateLimiterConfig.java) |
| 10 | Throttling | ⚠️ PARTIAL | Resilience4j circuit-breaker / retry on `gateway-service` and `order-service`; bulkheads are a roadmap item |
| 11 | Pagination | ✅ NEW | [PageQuery](common-service/src/main/java/com/services/common/pagination/PageQuery.java) + [PagedResponse](common-service/src/main/java/com/services/common/pagination/PagedResponse.java) — applied in `ProductController.list()` |
| 12 | Caching | ⏳ ROADMAP | Spring Cache + Redis on product-service — see [ROADMAP.md §1](ROADMAP.md#1-caching-12) |
| 13 | Idempotency | ⏳ ROADMAP | `Idempotency-Key` header on POST endpoints — see [ROADMAP.md §2](ROADMAP.md#2-idempotency-13) |
| 14 | Webhooks | ⏳ ROADMAP | notification-service extension — see [ROADMAP.md §3](ROADMAP.md#3-webhooks-14) |
| 15 | API Versioning | ✅ NEW | All controllers now under `/api/v1/<resource>` (plural). Gateway routes match. |
| 16 | OpenAPI | ✅ NEW | [CommonOpenApiAutoConfiguration](common-service/src/main/java/com/services/common/openapi/CommonOpenApiAutoConfiguration.java) — auto-applies to any servlet service depending on `common-service` |
| 17 | REST vs GraphQL | 📄 DOC | Discussed in [ROADMAP.md §6](ROADMAP.md#6-rest-vs-graphql-17). REST chosen for this codebase; reasons documented |
| 18 | API Gateway | ✅ FULL | [gateway-service](gateway-service/) — Spring Cloud Gateway + TokenRelay + CircuitBreaker + RateLimiter |
| 19 | Microservices | ✅ FULL | 12 modules, Eureka discovery, Config Server, Spring Cloud Gateway |
| 20 | Error Handling | ✅ NEW | [GlobalExceptionHandler](common-service/src/main/java/com/services/common/error/GlobalExceptionHandler.java) — RFC 7807 ProblemDetail across every servlet service |

✅ FULL = working today. ⚠️ PARTIAL = partial. ⏳ ROADMAP = planned next. 📄 DOC = explained, not built.

---

## What's new in this iteration (5 items, deeply implemented)

### 1. Global Error Handling — #20

**Pattern:** RFC 7807 `application/problem+json` responses for every error.

**Why:** Inconsistent error shapes across microservices is a common drift. RFC 7807 is the IETF standard and Spring 6 has first-class support via `org.springframework.http.ProblemDetail`.

**How it's wired:**

- [`GlobalExceptionHandler`](common-service/src/main/java/com/services/common/error/GlobalExceptionHandler.java) — `@RestControllerAdvice` mapping ~10 distinct exception types to specific HTTP statuses with a uniform shape.
- [`CommonErrorHandlingAutoConfiguration`](common-service/src/main/java/com/services/common/error/CommonErrorHandlingAutoConfiguration.java) — registered via [`META-INF/spring/.../AutoConfiguration.imports`](common-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports). Activates only on servlet apps, conditional on the bean missing — services can override with their own.
- Custom exceptions: [`ResourceNotFoundException`](common-service/src/main/java/com/services/common/error/exception/ResourceNotFoundException.java), [`BadRequestException`](common-service/src/main/java/com/services/common/error/exception/BadRequestException.java), [`ConflictException`](common-service/src/main/java/com/services/common/error/exception/ConflictException.java).

**Sample response:**

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "https://api.services.com/problems/resource-not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product with id 'missing' was not found",
  "instance": "/api/v1/products/missing",
  "timestamp": "2026-05-03T17:30:42.000+05:30",
  "resourceType": "Product",
  "identifier": "missing"
}
```

**To use in a new service:** add `<dependency>com.services:common-service</dependency>`. Done — auto-configuration picks it up.

### 2. OpenAPI everywhere — #16

**Pattern:** every servlet service auto-exposes `/v3/api-docs` (JSON) and `/swagger-ui.html` (interactive UI).

**Why:** Each service is its own product surface. Independent OpenAPI specs let teams generate clients, run contract tests, and let API consumers self-serve.

**How:** [`CommonOpenApiAutoConfiguration`](common-service/src/main/java/com/services/common/openapi/CommonOpenApiAutoConfiguration.java) registers a default `OpenAPI` bean keyed off `spring.application.name`. Bearer-JWT scheme is pre-declared so Swagger UI's "Authorize" button works without per-service config. Services can override with their own `@Bean OpenAPI` if needed.

**To customize per-service:** override `info.app.description`, `info.app.version`, `info.app.gateway-url` in `application.yaml`.

### 3. Pagination — #11

**Pattern:** every list endpoint uses `?page=&size=&sort=` and returns a uniform `PagedResponse<T>` envelope.

**Why:** Spring Data's native `Page` JSON shape isn't a stable contract — its fields drift across versions and include internals like `pageable.unsorted`. The wrapper isolates the public contract from Spring internals.

**How:**

- [`PageQuery`](common-service/src/main/java/com/services/common/pagination/PageQuery.java) — bind query params with `@ParameterObject` (so OpenAPI flattens them as separate query params, not a nested object). Caps `size` at 200 to prevent runaway queries.
- [`PagedResponse<T>`](common-service/src/main/java/com/services/common/pagination/PagedResponse.java) — record envelope with `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Spring Data `Page` → `PagedResponse` via `PagedResponse.of(page)`.

**Example:**

```bash
curl "http://localhost:8080/api/v1/products?page=0&size=20&sort=name,asc"
```

Returns:

```json
{
  "content": [ { "id": "...", "name": "...", "price": ... }, ... ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

### 4. API Versioning — #15

**Pattern:** every controller path is `/api/v1/<resource>` (plural noun, kebab-case if multi-word).

**Why:** URL versioning is the most-supported, easiest-to-cache, simplest-to-document strategy. Header-based versioning is technically purer but breaks tools like Swagger UI and complicates routing rules.

**Standardized:**
- product-service → `/api/v1/products` (was `/api/product`)
- order-service → `/api/v1/orders` (was `/api/order`)
- inventory-service → `/api/v1/inventory` (was `/api/inventory`)

**When v2 ships:** add `/api/v2/products` controllers alongside `/api/v1/products`. Don't deprecate v1 immediately — declare a sunset window in `Sunset` and `Deprecation` response headers (RFC 8594), then remove after the window.

### 5. Rate Limiting — #9

**Pattern:** Spring Cloud Gateway's `RequestRateLimiter` filter, backed by Redis, with two key strategies (per-IP for anonymous, per-user for authenticated).

**Why:**
- Per-IP for public reads — protects anonymous endpoints from abuse without auth.
- Per-user for writes — tokens-not-IPs prevents NAT'd users from sharing buckets, and stops a single compromised token from hammering writes.

**Limits chosen:**

| Route | Methods | Key | Replenish | Burst |
|---|---|---|---|---|
| `/api/v1/products/**` | GET | per-IP | 50/sec | 100 |
| `/api/v1/products/**` | POST/PUT/PATCH/DELETE | per-user | 5/sec | 10 |
| `/articles/protected` | any | per-user | 10/sec | 20 |

Exceeding the limit returns **HTTP 429 Too Many Requests** with `X-RateLimit-Remaining: 0` and a `Retry-After` header.

**How:** [`RateLimiterConfig`](gateway-service/src/main/java/com/services/gateway/ratelimit/RateLimiterConfig.java) provides two `KeyResolver` beans. Routes pick which one via `key-resolver: '#{@ipKeyResolver}'` or `'#{@userKeyResolver}'`. Token bucket state lives in Redis (already provisioned for sessions).

---

## Testing

### What we run

```bash
# Compile + package every module (the safe sanity check before commits)
mvn -B -DskipTests clean package

# Run the unit/slice tests for the controller (the meaningful coverage of this iteration)
mvn -B -pl product-service -am test
```

### Test results (2026-05-03)

| Module | Tests | Passing | Skipped | Failing | Notes |
|---|---|---|---|---|---|
| common-service | 1 | 1 | 0 | 0 | context-loads test |
| product-service | 9 | 8 | 1 | 0 | 8 new slice tests + 1 opt-in integration test |
| Other modules | various | mixed | — | mixed | Pre-existing skeleton context-load tests, some broken by Spring AI / OpenAI dep drift. **Out of scope for this iteration.** Tracked in [ROADMAP.md §7](ROADMAP.md) |

### The 8 ProductControllerTest cases

Each one exercises a specific pattern in this iteration:

| # | Test | Pattern proved |
|---|---|---|
| 1 | `create_returns201_withLocationHeader_andBody` | Status code 201 + `Location:` header on POST |
| 2 | `create_withInvalidBody_returns400_problemDetail` | RFC 7807 ProblemDetail on validation failure |
| 3 | `list_returnsPagedEnvelope` | `PagedResponse` shape, fields, defaults |
| 4 | `list_withSizeAboveCap_returns400` | `PageQuery` `size <= 200` cap |
| 5 | `getById_whenMissing_returns404_problemDetail` | `ResourceNotFoundException` → 404 ProblemDetail with extra fields |
| 6 | `put_replacesProduct_returns200` | PUT semantics |
| 7 | `patch_partialUpdate_returns200` | PATCH semantics |
| 8 | `delete_returns204` | DELETE returns 204 No Content |

### Running the integration test (Testcontainers)

```bash
# macOS / Colima users — DOCKER_HOST is forwarded to surefire via parent pom
DOCKER_HOST=unix:///${HOME}/.colima/default/docker.sock \
  mvn -pl product-service test -Dintegration.tests=true

# Docker Desktop users
mvn -pl product-service test -Dintegration.tests=true
```

The test spins up a real MongoDB container and validates the create flow end-to-end. Skipped by default (`@EnabledIfSystemProperty`) so the test suite stays portable across CI environments.

---

## Patterns NOT yet applied across all modules

This iteration applied the patterns deeply to **product-service** as a reference implementation. Other modules:

- **order-service / inventory-service**: only the `/api/v1/<resource>` versioning was applied. Still need pagination, full HTTP methods, OpenAPI annotations on each operation, depend on common-service. See [ROADMAP.md §4](ROADMAP.md).
- **resource-server**: not yet wired into common-service.
- **authorization-service**: already has its own `SwaggerConfig`. May want to migrate to the auto-configured one for consistency.

Doing this is mechanical and well-described in ROADMAP.md.

---

## Build verification (all green)

```text
mvn -B -DskipTests clean package
[INFO] microservices ............. SUCCESS
[INFO] integration-service ....... SUCCESS
[INFO] authorization-service ..... SUCCESS
[INFO] common-service ............ SUCCESS
[INFO] config-server ............. SUCCESS
[INFO] discovery-service ......... SUCCESS
[INFO] netflix-service ........... SUCCESS
[INFO] gateway-service ........... SUCCESS
[INFO] resource-server ........... SUCCESS
[INFO] product-service ........... SUCCESS
[INFO] order-service ............. SUCCESS
[INFO] notification-service ...... SUCCESS
[INFO] inventory-service ......... SUCCESS
[INFO] BUILD SUCCESS
```

```text
mvn -B -pl product-service -am test
common-service tests:    1/1 passed
product-service tests:   8/8 passed (1 opt-in integration test skipped)
BUILD SUCCESS
```
