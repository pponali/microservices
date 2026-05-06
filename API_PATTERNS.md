# API Patterns — The 20 Concepts, Where and How

This document walks through each of the 20 industry-standard API concepts and
shows **exactly** where and how it's implemented in this codebase. For each
concept you get:

1. **What it is** — one-paragraph definition
2. **Why it matters** — what breaks without it
3. **Where it lives in this repo** — file paths
4. **How it's implemented** — annotated code snippets
5. **How to test or use it** — concrete commands

If you only need a status summary, see [ROADMAP.md](ROADMAP.md). For runtime
infrastructure setup, see [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Table of contents

| # | Concept | Quick link |
|---|---|---|
| 1 | Endpoint | [§1](#1-endpoint) |
| 2 | HTTP Methods | [§2](#2-http-methods) |
| 3 | Request–Response | [§3](#3-requestresponse) |
| 4 | Status Codes | [§4](#4-status-codes) |
| 5 | Authentication | [§5](#5-authentication) |
| 6 | Authorization | [§6](#6-authorization) |
| 7 | Access Tokens | [§7](#7-access-tokens) |
| 8 | OAuth 2.0 | [§8](#8-oauth-20) |
| 9 | Rate Limiting | [§9](#9-rate-limiting) |
| 10 | Throttling | [§10](#10-throttling) |
| 11 | Pagination | [§11](#11-pagination) |
| 12 | Caching | [§12](#12-caching) |
| 13 | Idempotency | [§13](#13-idempotency) |
| 14 | Webhooks | [§14](#14-webhooks) |
| 15 | API Versioning | [§15](#15-api-versioning) |
| 16 | OpenAPI | [§16](#16-openapi) |
| 17 | REST vs GraphQL | [§17](#17-rest-vs-graphql) |
| 18 | API Gateway | [§18](#18-api-gateway) |
| 19 | Microservices | [§19](#19-microservices) |
| 20 | Error Handling | [§20](#20-error-handling) |

---

## 1. Endpoint

**What it is.** A single addressable operation on the server — a (URL path, HTTP method) pair that does one thing.

**Why it matters.** Endpoints are the contract. Naming them well (resource-oriented, plural, kebab-case) makes the API self-documenting and lets clients reason about it without reading docs.

**Where it lives.** REST controllers exist across the runnable modules:
- [`ProductController`](product-service/src/main/java/com/services/product/controller/ProductController.java)
- [`OrderController`](order-service/src/main/java/com/services/order/controller/OrderController.java)
- [`InventoryController`](inventory-service/src/main/java/com/services/invetory/controller/InventoryController.java)
- [`WebhookController`](notification-service/src/main/java/com/services/notification/webhook/WebhookController.java)
- `SecurityController` in authorization-service · `ArticlesController` in resource-server

**How it's implemented.** Each controller follows the same shape: `/api/v1/<resource>` (plural noun), one class per resource, methods kept small and delegated to a service.

```java
@RestController
@RequestMapping("/api/v1/products")          // versioned, plural, kebab-case
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalogue management")
public class ProductController {
    private final ProductService productService;
    // ... HTTP methods below
}
```

**How to verify.** Once product-service is running:
```bash
curl http://localhost:8080/v3/api-docs | jq '.paths | keys'
# Lists every endpoint the service exposes.
```

---

## 2. HTTP Methods

**What it is.** GET / POST / PUT / PATCH / DELETE — the verb in a request, each carrying specific semantics (read / create / replace / partial-update / remove).

**Why it matters.** Methods aren't decorative — proxies, caches, retry libraries, and the Spring stack itself all rely on the semantics. GET is cacheable. PUT/DELETE are idempotent. POST isn't. Get this wrong and infrastructure built on these assumptions breaks.

**Where it lives.** [`ProductController`](product-service/src/main/java/com/services/product/controller/ProductController.java) demonstrates the full set:

| Verb | Path | Semantic |
|---|---|---|
| `POST` | `/api/v1/products` | Create — server assigns id |
| `GET` | `/api/v1/products` | List (paginated) |
| `GET` | `/api/v1/products/{id}` | Read one |
| `PUT` | `/api/v1/products/{id}` | Replace (full update) |
| `PATCH` | `/api/v1/products/{id}` | Partial update |
| `DELETE` | `/api/v1/products/{id}` | Remove |

**How it's implemented.**

```java
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    ProductResponse created = productService.create(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
}

@PutMapping("/{id}")
public ProductResponse replace(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
    return productService.replace(id, request);
}

@PatchMapping("/{id}")
public ProductResponse patch(@PathVariable String id, @Valid @RequestBody ProductPatchRequest patch) {
    return productService.patch(id, patch);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable String id) {
    productService.delete(id);
}
```

**Notes on PATCH.** Partial updates use a dedicated [`ProductPatchRequest`](product-service/src/main/java/com/services/product/dto/ProductPatchRequest.java) DTO with all-optional fields — using the same DTO as PUT would force callers to send every field on every PATCH, defeating the point.

**How to test.**
```bash
ID=abc-123
curl -X POST     http://localhost:8080/api/v1/products    -d '{...}'
curl             http://localhost:8080/api/v1/products/$ID
curl -X PUT      http://localhost:8080/api/v1/products/$ID -d '{...}'
curl -X PATCH    http://localhost:8080/api/v1/products/$ID -d '{"price":9.99}'
curl -X DELETE   http://localhost:8080/api/v1/products/$ID -i  # → 204
```

---

## 3. Request–Response

**What it is.** The data shapes flowing in (request body) and out (response body), typically as DTOs separate from your domain model.

**Why it matters.** DTOs are the public contract. Using your JPA entities directly leaks schema details (lazy proxies, internal IDs, audit columns) and ties API evolution to database evolution.

**Where it lives.**
- [`ProductRequest`](product-service/src/main/java/com/services/product/dto/ProductRequest.java) — create / replace
- [`ProductPatchRequest`](product-service/src/main/java/com/services/product/dto/ProductPatchRequest.java) — partial update
- [`ProductResponse`](product-service/src/main/java/com/services/product/dto/ProductResponse.java) — outbound
- [`OrderRequest`](order-service/src/main/java/com/services/order/dto/OrderRequest.java) · [`InventoryResponse`](order-service/src/main/java/com/services/order/dto/InventoryResponse.java)
- [`WebhookSubscriptionRequest`](notification-service/src/main/java/com/services/notification/webhook/WebhookSubscriptionRequest.java) / [`WebhookSubscriptionResponse`](notification-service/src/main/java/com/services/notification/webhook/WebhookSubscriptionResponse.java)
- [`PagedResponse<T>`](common-service/src/main/java/com/services/common/pagination/PagedResponse.java) — common envelope

**How it's implemented.** Lombok-built records-or-classes with Bean Validation (`@NotBlank`, `@Size`, `@DecimalMin`) and Swagger metadata (`@Schema`):

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
@Schema(description = "Create / full-replace product payload")
public class ProductRequest {

    @NotBlank @Size(min = 1, max = 200)
    @Schema(description = "Display name", example = "Mechanical keyboard")
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull @DecimalMin("0.01")
    @Schema(example = "129.99")
    private BigDecimal price;
}
```

The PATCH variant intentionally omits `@NotNull` on every field — partial updates leave fields untouched if not provided:

```java
@Data
@Schema(description = "Partial product update — only set the fields you want to change.")
public class ProductPatchRequest {
    @Size(min = 1, max = 200) private String name;
    @Size(max = 2000)         private String description;
    @DecimalMin("0.01")       private BigDecimal price;
}
```

**Service-side mapping.** The service layer translates DTO ↔ entity:

```java
private ProductResponse toResponse(Product p) {
    return ProductResponse.builder()
            .id(p.getId()).name(p.getName())
            .description(p.getDescription()).price(p.getPrice())
            .build();
}
```

**How to verify validation runs.** A POST with an invalid body returns RFC 7807:

```bash
curl -i -X POST http://localhost:8080/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"","price":null}'
# HTTP/1.1 400
# Content-Type: application/problem+json
# {"title":"Validation Failed","status":400,"errors":[{"field":"name",...},{"field":"price",...}]}
```

---

## 4. Status Codes

**What it is.** The 3-digit HTTP code that classifies the response: success (2xx), redirect (3xx), client error (4xx), server error (5xx).

**Why it matters.** Status codes are the first thing tooling looks at — alerts, retries, circuit breakers, browser navigation. Returning `200 OK` with `{"success": false}` body is a common anti-pattern that breaks all of them.

**Where it lives.** Status codes are emitted in two places:

1. **Explicit on success paths** — controllers use `ResponseEntity.created(...)`, `@ResponseStatus(NO_CONTENT)`, etc.
2. **Centralised on error paths** — [`GlobalExceptionHandler`](common-service/src/main/java/com/services/common/error/GlobalExceptionHandler.java) maps each exception type to a specific status.

**Coverage in this project:**

| Code | Where it's returned | Trigger |
|---|---|---|
| `200 OK` | every read | normal `GET` |
| `201 Created` | `POST /api/v1/products`, `POST /api/v1/webhooks` | resource created — `Location` header set |
| `204 No Content` | `DELETE /api/v1/products/{id}`, `DELETE /api/v1/webhooks/{id}` | resource removed |
| `400 Bad Request` | `BadRequestException`, validation failures (`MethodArgumentNotValidException`) | malformed input |
| `401 Unauthorized` | resource-server, missing/invalid JWT | (auto from Spring Security) |
| `403 Forbidden` | `AccessDeniedException` | authenticated but lacking authority |
| `404 Not Found` | `ResourceNotFoundException`, `NoHandlerFoundException` | resource id doesn't exist; route doesn't exist |
| `405 Method Not Allowed` | `HttpRequestMethodNotSupportedException` | wrong verb on a known route |
| `409 Conflict` | `ConflictException`, `DataIntegrityViolationException`, idempotency concurrent | duplicate, lock fail, in-flight key |
| `422 Unprocessable Entity` | idempotency key reused with different body | semantically-invalid replay |
| `429 Too Many Requests` | gateway `RequestRateLimiter` | rate limit exceeded |
| `500 Internal Server Error` | catch-all in `GlobalExceptionHandler` | uncaught exceptions |

**How it's implemented.**

```java
// Success — 201 + Location, the canonical create response
@PostMapping
public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
    ProductResponse created = productService.create(req);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
}

// Success — 204 No Content (no body) on delete
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable String id) { ... }

// Failure — let an exception bubble; the global handler picks the status:
public ProductResponse getById(String id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
}
```

The handler:
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
    return problem(HttpStatus.NOT_FOUND, "resource-not-found", "Resource Not Found",
                   ex.getMessage(), req);
}
```

---

## 5. Authentication

**What it is.** Proving *who* the caller is. Typically: username + password → server issues a token → caller presents the token on every subsequent request.

**Why it matters.** Authentication is the gateway to authorization. Get this layer wrong and the rest of your security model is decorative.

**Where it lives.** [`authorization-service`](authorization-service/) is the authority. It's a Spring Authorization Server that:
- Stores users in MySQL via [`SecurityUser`](authorization-service/src/main/java/com/services/authorization/entity/SecurityUser.java) entity
- Authenticates them at `/oauth2/authorize` (browser) or `/oauth2/token` (programmatic)
- Issues RSA-signed JWTs as proof of authentication

**Configuration:** [`AuthorizationServerConfig`](authorization-service/src/main/java/com/services/authorization/config/AuthorizationServerConfig.java) and [`SecurityConfig`](authorization-service/src/main/java/com/services/authorization/config/SecurityConfig.java).

**The flow** (Authorization Code grant):

```
Browser           oauth2-client (5555)        authorization-service (8080)
   │                     │                                │
   │── visit / ─────────▶│                                │
   │                     │── 302 to /oauth2/authorize ───▶│
   │◀─────────── redirect ──────────────────────────────  │
   │── follow ──────────────────────────────────────────▶ │
   │◀── login form ──────────────────────────────────────│
   │── username + password ─────────────────────────────▶ │
   │                                            (looks up
   │                                             SecurityUser
   │                                             checks PW
   │                                             redirects)
   │◀── 302 with code=... ──────────────────────────────  │
   │── exchange code at /oauth2/token (POST) ───────────▶ │
   │                                            (issues JWT)
   │◀── access_token + refresh_token + id_token ─────────│
   │                                                      │
```

**How to test programmatically** (no browser, client-credentials grant):

```bash
curl -X POST -u api-client:secret http://localhost:8080/oauth2/token \
  -d 'grant_type=client_credentials&scope=read'
# {"access_token":"eyJraWQ...", "scope":"read", "token_type":"Bearer", "expires_in":300}
```

---

## 6. Authorization

**What it is.** Deciding *what* an authenticated caller is allowed to do. Roles, scopes, permissions.

**Why it matters.** Authentication says "this is Alice". Authorization says "Alice can read articles but cannot delete them".

**Where it lives.** Two complementary layers:

1. **`@PreAuthorize` on controller methods** — fine-grained, expression-based. Used in [`ArticlesController`](resource-server/src/main/java/com/services/resourceserver/controller/ArticlesController.java) (resource-server).
2. **HTTP-level filter chain** — coarse-grained, URL-pattern based. Configured in resource-server's security config.

**How it's implemented.**

```java
// Method-level
@RestController
@RequestMapping("/articles")
public class ArticlesController {

    @PreAuthorize("hasAuthority('SCOPE_read')")
    @GetMapping("/protected")
    public List<Article> getProtectedArticles() { ... }

    @PreAuthorize("hasRole('ROLE_ADMIN') and #userId == authentication.name")
    @DeleteMapping("/user/{userId}/article/{id}")
    public void delete(@PathVariable String userId, @PathVariable String id) { ... }
}
```

`SCOPE_read` matches the `read` scope embedded in the JWT (Spring prefixes scopes with `SCOPE_` automatically).

**HTTP-level — the filter chain.** Resource-server's security config validates JWTs against authorization-service's JWK set on every request:

```java
http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/articles/public").permitAll()
        .anyRequest().authenticated()
    );
```

**Failure modes** — `GlobalExceptionHandler` translates security failures to RFC 7807:

```java
@ExceptionHandler(AccessDeniedException.class)
public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
    return problem(HttpStatus.FORBIDDEN, "access-denied", "Access Denied",
                   "You do not have permission to perform this action", req);
}
```

**How to test:**

```bash
# No token — 401
curl -i http://localhost:8090/articles/protected

# Token without 'read' scope — 403
TOKEN=$(curl -s -X POST -u api-client:secret \
  http://localhost:8080/oauth2/token \
  -d 'grant_type=client_credentials&scope=write' | jq -r .access_token)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8090/articles/protected
# HTTP/1.1 403

# Token with 'read' scope — 200
TOKEN=$(curl -s -X POST -u api-client:secret \
  http://localhost:8080/oauth2/token \
  -d 'grant_type=client_credentials&scope=read' | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/articles/protected
```

---

## 7. Access Tokens

**What it is.** A short-lived credential the caller presents to access protected resources. Modern stacks use **JWT** (JSON Web Token): a compact, signed token containing the caller's identity and authorities.

**Why it matters.** JWTs are stateless — the server doesn't have to look up sessions on every request. The signature alone proves authenticity. This makes them ideal for microservices, where you don't want every service round-tripping to a session store.

**Where it lives.**

- **Issuer:** [`authorization-service`](authorization-service/) — RSA-signs JWTs at `/oauth2/token`. JWK source generated at startup; rotate by replacing the `JWKSource<SecurityContext>` bean in [`AuthorizationServerConfig`](authorization-service/src/main/java/com/services/authorization/config/AuthorizationServerConfig.java).
- **Verifier:** [`resource-server`](resource-server/) — pulls the issuer's JWK set at startup, caches the public keys, validates signatures on each request.
- **Bearer:** [`oauth2-client`](oauth2-client/) and any client app — sends `Authorization: Bearer <jwt>` with each call.
- **Pass-through:** [`gateway-service`](gateway-service/) — `TokenRelay` filter forwards the JWT from the user's session to downstream services.

**JWT shape** (decode at jwt.io):

```
Header:    { "alg": "RS256", "typ": "JWT", "kid": "abc-123" }
Payload:   { "sub": "alice",
             "iss": "http://auth-server:8080",
             "aud": "api-client",
             "exp": 1714723500,
             "iat": 1714723200,
             "scope": "read write",
             "authorities": ["ROLE_USER"] }
Signature: <RSA-PSS over header.payload using the issuer's private key>
```

**Verification — what the resource-server does on every request:**

```
1. Extract Bearer token from Authorization header
2. Decode header → look up `kid` (key id)
3. Fetch JWK with that kid from auth-server's /oauth2/jwks (cached)
4. Verify RS256 signature using the JWK's public key
5. Check exp / iat / nbf
6. Build a Spring Security Authentication with scopes as authorities
7. @PreAuthorize sees those authorities and decides
```

**Why RSA, not HMAC.** RSA (asymmetric) lets the resource server verify with only the public key. HMAC (symmetric) requires every verifier to hold the signing secret — fine for monoliths, dangerous for microservices.

**Token expiration & refresh.** Access tokens are short (5 minutes by default in this project). Long-lived **refresh tokens** are also issued; they're used at `/oauth2/token` with `grant_type=refresh_token` to mint a fresh access token without re-prompting the user.

---

## 8. OAuth 2.0

**What it is.** The IETF authorization framework. It's not a single protocol — it's a family of "grants" (flows) each suited to a different scenario.

**Grants in this project:**

| Grant | When to use | Where |
|---|---|---|
| **Authorization Code** | Browser-based apps acting on a user's behalf | oauth2-client → auth-server → user logs in → code exchanged for token |
| **Client Credentials** | Service-to-service calls (no user) | resource-server-to-other-service, automation scripts |
| **Refresh Token** | Mint a new access token without re-auth | Long-lived sessions in oauth2-client |

**Where it lives** — the four pieces of an OAuth2 deployment, all present in this repo:

| Role | Module | What it does |
|---|---|---|
| **Authorization Server** | [`authorization-service`](authorization-service/) | Knows the users; issues tokens |
| **Resource Server** | [`resource-server`](resource-server/) | Owns the protected data; validates tokens |
| **Client** | [`oauth2-client`](oauth2-client/) | Acts on behalf of a user; holds tokens |
| **Gateway (relay)** | [`gateway-service`](gateway-service/) | Forwards user's tokens to downstream services |

**Key configuration files:**

- [`AuthorizationServerConfig.java`](authorization-service/src/main/java/com/services/authorization/config/AuthorizationServerConfig.java) — registered clients, token settings, JWK source.
- [`SecurityConfig.java`](authorization-service/src/main/java/com/services/authorization/config/SecurityConfig.java) — login form, password encoder, user details service.
- [`gateway-service/.../application.yaml`](gateway-service/src/main/resources/application.yaml) — `TokenRelay` filter forwards Bearer tokens downstream.
- [`oauth2-client/.../application.yml`](oauth2-client/src/main/resources/application.yml) — `spring.security.oauth2.client.registration.*` config.

**Authorization Code flow (browser):**

```
Visit oauth2-client (5555) → not authenticated
  ↓ 302
Redirect to auth-server /oauth2/authorize?client_id=...&response_type=code&...
  ↓
Login form on auth-server (8080)
  ↓ POST credentials
Validate → 302 with ?code=ABC
  ↓
oauth2-client receives code, POSTs to /oauth2/token
  ↓
auth-server returns { access_token, refresh_token, id_token }
  ↓
oauth2-client now uses the access_token to call APIs
```

**How the gateway relays it.** When the user is authenticated in the gateway via OAuth2 client config, every request through the gateway has the user's JWT injected as `Authorization: Bearer <jwt>` for downstream services — that's the `TokenRelay` filter:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - TokenRelay=    # forwards the user's JWT to every routed request
        - SaveSession    # persists session in Redis (so the JWT survives restarts)
```

---

## 9. Rate Limiting

**What it is.** Bounding the number of requests a single caller (IP, user, API key) can make over a time window. Protects against abuse, runaway clients, and brute-force attacks.

**Why it matters.** Without it, one buggy script can bring your service down. Rate limits are also a fairness mechanism in multi-tenant systems.

**Where it lives.** [`gateway-service`](gateway-service/), implemented via Spring Cloud Gateway's built-in `RequestRateLimiter` filter, backed by Redis.

**Files:**
- [`RateLimiterConfig.java`](gateway-service/src/main/java/com/services/gateway/ratelimit/RateLimiterConfig.java) — declares two `KeyResolver` beans.
- [`gateway-service/.../application.yaml`](gateway-service/src/main/resources/application.yaml) — applies the filter to specific routes.

**The algorithm:** Token bucket. Each `(key, route)` has a Redis-backed bucket. Each request consumes 1 token. Buckets refill at `replenishRate` tokens/sec up to `burstCapacity`. Empty bucket → HTTP 429.

**Two key-resolver strategies:**

```java
@Bean @Primary
public KeyResolver ipKeyResolver() {                 // public reads
    return exchange -> Mono.just(
        Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("anonymous"));
}

@Bean
public KeyResolver userKeyResolver() {                // authenticated writes
    return exchange -> exchange.getPrincipal()
        .map(p -> "user:" + p.getName())
        .switchIfEmpty(Mono.just("anonymous:" + ...));
}
```

**Why two strategies.** Public anonymous reads are commonly NAT'd — limiting per IP works. Authenticated writes should limit per user (JWT subject) so one compromised token can't burn through a whole NAT'd corporate IP's quota.

**Configured limits:**

| Route | Methods | Resolver | Replenish | Burst |
|---|---|---|---|---|
| `/api/v1/products/**` | `GET` | per-IP | 50 / s | 100 |
| `/api/v1/products/**` | `POST PUT PATCH DELETE` | per-user | 5 / s | 10 |
| `/articles/protected` | any | per-user | 10 / s | 20 |

**How to verify the limit:**

```bash
# Hammer the products list endpoint past the burst capacity (100)
for i in $(seq 1 200); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3333/api/v1/products
done | sort | uniq -c
# Expect:  ~100 200    100 429
```

The 429 response includes `X-RateLimit-Remaining: 0` and a `Retry-After` header.

---

## 10. Throttling

**What it is.** Adjacent to rate limiting but lower-level — bounding *concurrency* (how many in-flight calls), or shaping the rate of *outbound* calls so a slow downstream can't saturate caller threads.

**Why it matters.** Without throttling, a single slow downstream brings down its callers. Bulkheads are how you prevent cascading failures.

**Where it lives.** [`order-service`](order-service/) uses the full Resilience4j stack on its outbound call to `inventory-service`.

**Files:**
- [`OrderController.java`](order-service/src/main/java/com/services/order/controller/OrderController.java) — annotations
- [`order-service/.../application.properties`](order-service/src/main/resources/application.properties) — config

**The four layers, applied in this order:**

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@Bulkhead(name = "inventory")                                    // 1. concurrency cap
@CircuitBreaker(name = "inventory", fallbackMethod = "fallback") // 2. fail-fast on streak
@TimeLimiter(name = "inventory")                                 // 3. abort slow calls
@Retry(name = "inventory")                                       // 4. retry transient failures
public CompletableFuture<String> placeOrder(@RequestBody OrderRequest req) {
    return CompletableFuture.supplyAsync(() -> orderService.placeOrder(req));
}
```

**Configuration:**

```properties
# Bulkhead — caps concurrent in-flight calls. Industry rule:
#   bulkhead size <= downstream's accept capacity / number-of-callers
resilience4j.bulkhead.instances.inventory.maxConcurrentCalls=20
resilience4j.bulkhead.instances.inventory.maxWaitDuration=500ms

# Circuit breaker — fail-fast after a streak of failures
resilience4j.circuitbreaker.instances.inventory.slidingWindowSize=5
resilience4j.circuitbreaker.instances.inventory.failureRateThreshold=50
resilience4j.circuitbreaker.instances.inventory.waitDurationInOpenState=5s

# Time limiter — abort calls that take too long
resilience4j.timelimiter.instances.inventory.timeout-duration=3s

# Retry — transient failures
resilience4j.retry.instances.inventory.max-attempts=3
resilience4j.retry.instances.inventory.wait-duration=5s
```

**Why this order matters.** The annotations are evaluated outside-in (Bulkhead first, Retry last). So:
1. Bulkhead rejects the call if 20 are already in flight (no point starting a 21st)
2. CircuitBreaker rejects if recent failures are too high (fail fast)
3. TimeLimiter aborts the actual call if it takes >3s
4. Retry kicks in on a transient failure (timeout, connection refused)

Reverse the order and you'd retry inside the time limit (no), or count rejected-by-bulkhead as circuit-breaker failures (no).

**Difference from rate limiting (concept #9).** Rate limit = bound *incoming* request rate. Throttle = bound *concurrency* and *outbound* call shape. Same library family (Resilience4j), different problem.

---

## 11. Pagination

**What it is.** Returning lists in pages instead of all-at-once. Three contracts: how the client requests a page (`?page=&size=&sort=`), the page envelope, and how the server enforces sane limits.

**Why it matters.** Without pagination, your `GET /products` endpoint OOMs the server the day a tenant has 1M products. Plus, mobile clients on slow networks can't render 10k rows anyway.

**Where it lives.** Shared in [`common-service`](common-service/), applied wherever lists are returned.

**Files:**
- [`PageQuery.java`](common-service/src/main/java/com/services/common/pagination/PageQuery.java) — input DTO bound from query params
- [`PagedResponse.java`](common-service/src/main/java/com/services/common/pagination/PagedResponse.java) — output envelope
- [`ProductController.list()`](product-service/src/main/java/com/services/product/controller/ProductController.java) — usage example
- [`ProductService.findAll(Pageable)`](product-service/src/main/java/com/services/product/service/ProductService.java) — service-side `Page<T>` mapping

**The input DTO:**

```java
public class PageQuery {
    @Min(0)              private int    page = 0;
    @Min(1) @Max(200)    private int    size = 20;          // 200 hard cap
                         private String sort;                // "name,asc"

    public Pageable toPageable() {
        Sort spec = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            String[] tokens = sort.split(",");
            String field = tokens[0].trim();
            Sort.Direction dir = (tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim()))
                                 ? Sort.Direction.DESC : Sort.Direction.ASC;
            spec = Sort.by(dir, field);
        }
        return PageRequest.of(page, size, spec);
    }
}
```

The `@Max(200)` is the hard cap — clients can ask for up to 200 per page; bigger requests fail validation (handled by `GlobalExceptionHandler` → 400).

**The output envelope:**

```java
public record PagedResponse<T>(
    List<T>  content,
    int      page,
    int      size,
    long     totalElements,
    int      totalPages,
    boolean  first,
    boolean  last
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                                   page.getTotalElements(), page.getTotalPages(),
                                   page.isFirst(), page.isLast());
    }
}
```

**Why a custom envelope?** Spring Data's native `Page` JSON shape isn't a stable contract — its fields drift across Spring Boot versions and include internals like `pageable.unsorted` that callers shouldn't depend on. Wrapping isolates clients from Spring internals.

**The controller:**

```java
@GetMapping
public PagedResponse<ProductResponse> list(@Valid @ParameterObject PageQuery query) {
    return PagedResponse.of(productService.findAll(query.toPageable()));
}
```

`@ParameterObject` is springdoc's annotation that flattens `PageQuery` into individual `?page=&size=&sort=` query params in the OpenAPI spec (without it, OpenAPI would render it as a nested object, which doesn't match the wire format).

**Sample request:**

```bash
curl 'http://localhost:8080/api/v1/products?page=2&size=20&sort=name,asc'
```

**Sample response:**

```json
{
  "content": [ {...}, {...} ],
  "page": 2,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": false,
  "last": false
}
```

---

## 12. Caching

**What it is.** Storing the result of an expensive computation (DB read, RPC, render) so repeats are cheap.

**Why it matters.** A 5ms cache hit vs a 50ms DB hit per request adds up. Caching pushes latency down and reduces load on the DB. Done wrong, it serves stale data.

**Where it lives.** [`product-service`](product-service/) uses Spring Cache annotations backed by Redis.

**Files:**
- [`CacheConfig.java`](product-service/src/main/java/com/services/product/config/CacheConfig.java) — `RedisCacheManager` bean with TTL + JSON serialization
- [`ProductService.java`](product-service/src/main/java/com/services/product/service/ProductService.java) — `@Cacheable` / `@CacheEvict` / `@CachePut` annotations
- [`ProductServiceApplication.java`](product-service/src/main/java/com/services/product/ProductServiceApplication.java) — `@EnableCaching`
- [`ProductServiceCachingTest.java`](product-service/src/test/java/com/services/product/service/ProductServiceCachingTest.java) — verifies the wiring

**Strategy per method:**

```java
@CacheEvict(value = "products", allEntries = true)
public ProductResponse create(ProductRequest req) { ... }

@Cacheable(value = "products", key = "#id")
public ProductResponse getById(String id) {
    log.debug("Cache miss for {} — loading from DB", id);
    return productRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
}

public Page<ProductResponse> findAll(Pageable pageable) {        // intentionally NOT cached
    return productRepository.findAll(pageable).map(this::toResponse);
}

@CachePut(value = "products", key = "#id")
public ProductResponse replace(String id, ProductRequest req) { ... }

@CachePut(value = "products", key = "#id")
public ProductResponse patch(String id, ProductPatchRequest patch) { ... }

@CacheEvict(value = "products", key = "#id")
public void delete(String id) { ... }
```

**Why `findAll` isn't cached.** Caching paginated list responses is an anti-pattern: every `(page, size, sort)` combination is a separate cache entry, wasting memory, and they race with writes — invalidating "all list pages" on any write defeats the cache.

**The cache config:**

```java
RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))      // Always set a TTL
        .disableCachingNullValues()             // Don't cache misses
        .serializeKeysWith(StringRedisSerializer)
        .serializeValuesWith(GenericJackson2JsonRedisSerializer);
```

**Industry guardrails captured:**
- **Always set a TTL.** Caches without TTL are bug factories.
- **Disable caching nulls.** `Optional.empty()` returning `null` shouldn't poison the cache.
- **Cache the DTO, not the entity.** Caching JPA entities causes lifecycle headaches (lazy proxies after deserialization).
- **JSON serialization** (not JDK) so cache values are inspectable in `redis-cli`.

**Verify with the test:**
```bash
mvn -pl product-service -am test -Dtest=ProductServiceCachingTest \
  -Dsurefire.failIfNoSpecifiedTests=false
# 4 tests: cache hit, no-poison-on-miss, eviction on create, eviction on delete
```

**Verify in Redis:**
```bash
redis-cli
> KEYS products::*
> GET products::abc-123
```

---

## 13. Idempotency

**What it is.** A property of an operation: doing it twice has the same effect as doing it once. GET is naturally idempotent. POST isn't — but you can make it so via an `Idempotency-Key` header.

**Why it matters.** Networks fail. The client doesn't know if its POST got through; it retries; now you've created two orders. Idempotency keys make retry safe by design.

**Where it lives.** Implemented as a reusable filter in [`common-service`](common-service/), opt-in per service. Currently activated on [`order-service`](order-service/) where double-charging would be a real problem.

**Files:**
- [`IdempotencyFilter.java`](common-service/src/main/java/com/services/common/idempotency/IdempotencyFilter.java) — the servlet filter
- [`IdempotencyStore.java`](common-service/src/main/java/com/services/common/idempotency/IdempotencyStore.java) — storage interface
- [`RedisIdempotencyStore.java`](common-service/src/main/java/com/services/common/idempotency/RedisIdempotencyStore.java) — Redis impl
- [`IdempotencyAutoConfiguration.java`](common-service/src/main/java/com/services/common/idempotency/IdempotencyAutoConfiguration.java) — auto-wires when `services.idempotency.enabled=true` + Redis present
- [`IdempotencyFilterTest.java`](common-service/src/test/java/com/services/common/idempotency/IdempotencyFilterTest.java) — 7 tests

**The protocol** (Stripe-style):

```
1. Client sends:
     POST /api/v1/orders
     Idempotency-Key: 4cdf9a2e-...
     {body}

2. Filter computes:
     bucket = SHA-256(method | path | key)

3. Filter looks up bucket in Redis:

   - if record exists & body hash matches → REPLAY stored response
                                           with Idempotent-Replayed: true header
   - if record exists & body hash differs → 422 (key reuse with different body)
   - if no record:
       - tryReserve(bucket) via Redis SETNX
         - reserved → process; cache (status, headers, body) on 2xx/3xx
         - reservation conflict → 409 (concurrent in-flight request)
```

**Activation in order-service:**

```properties
services.idempotency.enabled=true
spring.data.redis.host=${REDIS_HOST:localhost}
```

**The filter's request handling, simplified:**

```java
String idempotencyKey = request.getHeader("Idempotency-Key");
if (idempotencyKey == null || !APPLICABLE_METHODS.contains(request.getMethod())) {
    chain.doFilter(request, response); return;
}

String compoundKey = sha256(method + "|" + uri + "|" + idempotencyKey);
String bodyHash    = sha256(requestBody);

Optional<IdempotencyRecord> existing = store.get(compoundKey);
if (existing.isPresent()) {
    replay(existing.get(), bodyHash, response);  // returns 200/201/422
    return;
}

if (!store.tryReserve(compoundKey, RESERVATION_TTL)) {
    response.setStatus(409);                      // concurrent in-flight
    return;
}

// Process the request, then cache the response.
chain.doFilter(wrappedRequest, wrappedResponse);
if (status >= 200 && status < 400) {
    store.put(compoundKey, recordOf(status, headers, body), Duration.ofHours(24));
}
```

**Why 24-hour TTL.** Stripe's idempotency window. Long enough for any reasonable client retry, short enough that storage doesn't grow unbounded.

**What's NOT idempotent here:**
- GET / HEAD / OPTIONS (idempotent at the HTTP-method level — filter bypasses them)
- 4xx / 5xx responses on the first call (we want clients to retry on transient failures)

**Test it manually:**

```bash
KEY=$(uuidgen)

# First call — creates the order, returns 201
curl -i -X POST http://localhost:8081/api/v1/orders \
  -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' \
  -d '{"orderLineItemsList":[{"skuCode":"abc","price":10,"quantity":1}]}'

# Second call with SAME key + same body — replays the original 201, no second order
curl -i -X POST http://localhost:8081/api/v1/orders \
  -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' \
  -d '{"orderLineItemsList":[{"skuCode":"abc","price":10,"quantity":1}]}'
# Note `Idempotent-Replayed: true` in the response headers

# Same key, DIFFERENT body — 422
curl -i -X POST http://localhost:8081/api/v1/orders \
  -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' \
  -d '{"orderLineItemsList":[{"skuCode":"different","price":99,"quantity":99}]}'
```

---

## 14. Webhooks

**What it is.** Outbound HTTP POSTs from your server to URLs the customer has registered, fired in response to events. The inverse of an API: the customer doesn't poll you; you push to them.

**Why it matters.** Real systems integrate via events (order placed, payment captured, inventory low). Webhooks are how you tell other systems something happened in real time. Done right, they're signed and retried; done wrong, they leak data and silently fail.

**Where it lives.** [`notification-service`](notification-service/), specifically the `webhook` package.

**Files:**
- [`WebhookSubscription.java`](notification-service/src/main/java/com/services/notification/webhook/WebhookSubscription.java) — JPA entity (eventType, callbackUrl, secret, enabled)
- [`WebhookController.java`](notification-service/src/main/java/com/services/notification/webhook/WebhookController.java) — `POST/GET/DELETE /api/v1/webhooks` for subscription management
- [`WebhookSigner.java`](notification-service/src/main/java/com/services/notification/webhook/WebhookSigner.java) — HMAC-SHA256 signing
- [`WebhookDeliveryService.java`](notification-service/src/main/java/com/services/notification/webhook/WebhookDeliveryService.java) — async delivery + retry
- [`WebhookSignerTest.java`](notification-service/src/test/java/com/services/notification/webhook/WebhookSignerTest.java) — 4 signature tests
- [`NotificationServiceApplication.OrderPlacedListener`](notification-service/src/main/java/com/services/notification/NotificationServiceApplication.java) — Kafka → webhook bridge

**Flow:**

```
order-service places order
    ↓ Kafka publish (OrderPlacedEvent)
notification-service consumes
    ↓ for each subscription matching "order.placed":
    ↓     - sign body with HMAC-SHA256
    ↓     - POST to callbackUrl
    ↓     - retry on failure: 10s → 30s → 2m → 10m → 1h → 6h
```

**Signature format** (Stripe-style):

```
X-Webhook-Signature: t=<unix_seconds>,v1=<hex_hmac_sha256>
X-Webhook-Event: order.placed
X-Webhook-Attempt: 1
```

The receiver verifies by re-computing HMAC-SHA256 over `<unix_seconds>.<raw_body>` with the shared secret and constant-time-comparing to `v1`.

**Why include the timestamp.** Replay protection: the receiver rejects deliveries older than ~5 minutes, even if the signature is correct. This stops captured webhook bodies from being replayed later.

**The signer:**

```java
public static String sign(byte[] body, String secret, long unixTimestamp) {
    String signedPayload = unixTimestamp + "." + new String(body, UTF_8);
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
    byte[] mac = hmac.doFinal(signedPayload.getBytes(UTF_8));
    return "t=" + unixTimestamp + ",v1=" + HexFormat.of().formatHex(mac);
}
```

**The delivery + retry loop:**

```java
private static final long[] BACKOFF_SECONDS = {10, 30, 120, 600, 3600, 21600};

private void deliverWithRetry(WebhookSubscription sub, byte[] body) {
    for (int attempt = 0; attempt < BACKOFF_SECONDS.length; attempt++) {
        if (attemptDelivery(sub, body, attempt + 1)) return;       // 2xx → done
        if (attempt < BACKOFF_SECONDS.length - 1) {
            TimeUnit.SECONDS.sleep(BACKOFF_SECONDS[attempt]);      // back off
        }
    }
    log.error("DLQ-worthy: webhook to {} failed after 6 attempts", sub.getCallbackUrl());
}
```

**Permanent vs transient failures:**

```java
if (statusCode.is4xxClientError() && statusCode.value() != 408 && statusCode.value() != 429) {
    log.warn("Permanent error {}; not retrying", statusCode);
    return true;        // don't burn retries on a 400 BadRequest
}
```

**Test by registering a subscription and watching it fire:**

```bash
# Register a webhook (point at requestbin.com or your own listener)
curl -X POST http://localhost:8082/api/v1/webhooks \
  -H 'Content-Type: application/json' \
  -d '{"eventType":"order.placed","callbackUrl":"https://en.0qjp25v8.requestbin-test.com",
       "secret":"a-strong-random-secret-string"}'

# Then place an order via order-service.
# Watch the requestbin URL — you should see:
#   POST <body>
#   X-Webhook-Signature: t=...,v1=...
#   X-Webhook-Event: order.placed
```

---

## 15. API Versioning

**What it is.** A way to evolve the API without breaking existing clients. URL versioning (`/api/v1/`), header versioning (`Accept: application/vnd.x.v2+json`), or content-type versioning are the three options.

**Why it matters.** Once your API is public, you can't change the shape of `/products` without breaking integrations. Versioning gives you a coexistence window.

**Where it lives.** Every controller, on every module, uses **URL versioning**:

```
/api/v1/products
/api/v1/orders
/api/v1/inventory
/api/v1/webhooks
```

**Why URL versioning** (vs the alternatives):
- **Most-supported**: every HTTP client, browser, proxy, and tool understands it without extra config.
- **Cacheable**: HTTP caches use URL as the cache key. Header-versioned URLs would all share one cache entry (broken).
- **Documentable**: Swagger UI shows distinct versions cleanly.

**The convention** (used everywhere in this repo):

```java
@RestController
@RequestMapping("/api/v1/products")    // version, plural noun
public class ProductController { ... }
```

**When v2 ships.** Add `/api/v2/products` controllers alongside `/api/v1/products`. Don't deprecate v1 immediately — declare a sunset window in HTTP response headers per RFC 8594:

```
Sunset: Sun, 01 Sep 2026 00:00:00 GMT
Deprecation: true
Link: </api/v2/products>; rel="successor-version"
```

Then remove v1 after the window.

**Gateway routing.** The gateway routes are also versioned:

```yaml
- id: products-list
  uri: lb://product-service
  predicates:
    - Path=/api/v1/products/**
```

When v2 ships, add `/api/v2/products/**` route → `lb://product-service` (or to a different service if v2 is implemented as a sibling).

---

## 16. OpenAPI

**What it is.** A machine-readable spec describing your API: endpoints, request/response shapes, status codes, auth schemes. The de-facto standard for API documentation. JSON or YAML.

**Why it matters.** Swagger UI gives clients an interactive playground. Code generators emit clients in 30+ languages from the same spec. Contract tests validate breaking changes.

**Where it lives.** Auto-configured for **every servlet service** that depends on `common-service`. Each service exposes:
- `/v3/api-docs` — JSON spec
- `/swagger-ui.html` — interactive UI

**Files:**
- [`CommonOpenApiAutoConfiguration.java`](common-service/src/main/java/com/services/common/openapi/CommonOpenApiAutoConfiguration.java) — auto-config, conditional on classpath
- [`META-INF/spring/.../AutoConfiguration.imports`](common-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) — registration
- Per-controller annotations: `@Tag`, `@Operation`, `@ApiResponses` — see [`ProductController.java`](product-service/src/main/java/com/services/product/controller/ProductController.java)

**The auto-config:**

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class CommonOpenApiAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public OpenAPI commonOpenApi(@Value("${spring.application.name:service}") String app, ...) {
        return new OpenAPI()
                .info(new Info().title(app + " API").version(version)
                        .contact(new Contact().name("Platform Team").email("..."))
                        .license(new License().name("Apache 2.0")...))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("Gateway"),
                        new Server().url("/").description("Direct (this service)")
                ))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

`@ConditionalOnMissingBean` means a service can override with its own `@Bean OpenAPI` if it wants something more specific.

**Per-controller annotations:**

```java
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalogue management")
public class ProductController {

    @Operation(summary = "Create a new product")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "409", description = "Duplicate")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) { ... }
}
```

**Per-DTO annotations:**

```java
@Schema(description = "Display name", example = "Mechanical keyboard")
private String name;
```

**The "Authorize" button.** The bearer-JWT scheme declared in the auto-config makes Swagger UI show an Authorize button — paste your JWT once, every test call carries it.

**View it:**

```bash
# JSON spec
curl http://localhost:8080/v3/api-docs | jq

# Interactive UI
open http://localhost:8080/swagger-ui.html
```

---

## 17. REST vs GraphQL

**What it is.** Two competing paradigms for API design. REST = resources + HTTP verbs. GraphQL = a single endpoint with a query language; clients ask for exactly what they want.

**Why it's listed.** This concept isn't an implementation — it's a choice you make once per service. This project chose **REST**, deliberately.

**Trade-off summary in this codebase's context:**

| Dimension | REST (current) | GraphQL |
|---|---|---|
| Mobile bandwidth | Multiple round trips, possible over-fetching | Single round trip, exact fields |
| Caching | Trivial — HTTP layer + Redis on URLs | Hard — POST bodies vary; needs persisted-queries |
| Versioning | URL versioning works | Schema evolution + deprecations harder to reason about |
| Tooling | OpenAPI, Swagger UI, codegen in 30+ langs | GraphiQL, codegen — newer ecosystem |
| Auth | Mature: Bearer JWT, scopes, per-endpoint | Field-level authorization at the resolver layer — easy to leak data |
| Performance debugging | One request → one trace | One request → N resolver calls; needs DataLoader for N+1 |
| Microservices fit | Each service owns its REST surface | Federated GraphQL is operationally complex |

**Recommendation for this project:** stay on REST. The codebase has 13 services with mixed data stores. Adding a federated GraphQL gateway adds operational complexity (federation gateway, schema registry, observability of resolvers) without clear payoff for what is currently a server-to-server architecture.

**When to revisit:** if a mobile/web product team starts complaining about over-fetching or N+1 round trips, prototype an Apollo Federation gateway over 2–3 services. If it survives a quarter, commit.

**To actually add GraphQL** (out of scope for this project but possible):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

Then `@SchemaMapping` / `@QueryMapping` annotations on a service. See https://spring.io/projects/spring-graphql.

---

## 18. API Gateway

**What it is.** A single entry point that fronts your microservices. Routes, authenticates, throttles, transforms, and monitors traffic before it reaches the back-end.

**Why it matters.** Without a gateway, every service has to re-implement auth, rate limiting, CORS, request logging, etc. A gateway centralises cross-cutting traffic concerns and lets back-end services focus on business logic.

**Where it lives.** [`gateway-service`](gateway-service/) — Spring Cloud Gateway (reactive / WebFlux).

**Files:**
- [`GatewayApplication.java`](gateway-service/src/main/java/com/services/gateway/GatewayApplication.java)
- [`gateway-service/.../application.yaml`](gateway-service/src/main/resources/application.yaml) — routes
- [`RateLimiterConfig.java`](gateway-service/src/main/java/com/services/gateway/ratelimit/RateLimiterConfig.java) — KeyResolvers
- [`LoggingGlobalPreFilter.java`](gateway-service/src/main/java/com/services/gateway/filter/LoggingGlobalPreFilter.java) — logging

**What this gateway does:**

| Concern | How |
|---|---|
| **Routing** | `Path=/api/v1/products/**` predicate → `lb://product-service` (load-balanced via Eureka) |
| **OAuth2 client** | Browser sessions terminated at the gateway; redirects to authorization-service for login |
| **Token relay** | `TokenRelay` filter forwards the user's JWT to downstream services as `Authorization: Bearer ...` |
| **Sessions** | Stored in Redis (`SaveSession` filter); JWTs survive gateway restarts |
| **Rate limiting** | Redis token-bucket per route, per IP or per user |
| **Circuit breaking** | `Resilience4j CircuitBreaker` filter on routes that hit flaky downstreams |
| **Retry** | `Retry` filter with exponential backoff on transient failures |

**Sample route showing all the filters in action:**

```yaml
- id: protected-articles
  uri: http://localhost:8090
  predicates:
    - Path=/articles/protected
  filters:
    - name: CircuitBreaker
      args:
        name: resourceServer
        fallbackUri: forward:/fallback
    - name: Retry
      args:
        retries: 30
        methods: GET
        backoff:
          firstBackoff: 10ms
          maxBackoff: 500ms
    - name: RequestRateLimiter
      args:
        key-resolver: '#{@userKeyResolver}'
        redis-rate-limiter.replenishRate: 10
        redis-rate-limiter.burstCapacity: 20
```

**Why server-side LB** (`lb://service-name`) **vs hard-coded URLs**: when product-service scales to 3 replicas, the gateway distributes requests across them via Spring Cloud LoadBalancer + Eureka. Hard-coded URLs would pin all traffic to one host.

---

## 19. Microservices

**What it is.** An architectural style where a system is built as a suite of small, independently deployable services that communicate over the network.

**Why it matters.** Each service is owned by one team, deployed on its own cadence, and scaled independently. Trade-off: distributed-system problems (network failures, partial outages, eventual consistency) become your problem.

**Where it lives.** This repo IS a microservices architecture. The 13 modules + their wiring:

| | Module | Boundary |
|---|---|---|
| Edge | gateway-service | Single ingress |
| Identity | authorization-service · resource-server · oauth2-client | OAuth2 stack |
| Discovery | discovery-service | Eureka |
| Config | config-server | Externalised config |
| Domain | product-service · order-service · inventory-service · notification-service | Business logic |
| Library | common-service | Shared cross-cutting code |
| Demo | integration-service · netflix-service | Sandboxes |

**Patterns the project demonstrates:**

| Pattern | How |
|---|---|
| **Service discovery** | Every service has `spring-cloud-starter-netflix-eureka-client`; registers at startup, queries to find others |
| **Client-side load balancing** | Resilience4j Spring Cloud LoadBalancer picks instances; `lb://service-name` URIs |
| **Externalised config** | config-server (Spring Cloud Config) — wired but not yet consumed |
| **Resilience patterns** | Resilience4j: CircuitBreaker, TimeLimiter, Bulkhead, Retry on order → inventory |
| **Event-driven communication** | Kafka: order-service publishes `OrderPlacedEvent`, notification-service consumes |
| **API gateway** | Spring Cloud Gateway as the single ingress |
| **Distributed sessions** | Redis-backed Spring Session in the gateway |

**Anti-patterns the project deliberately avoids:**

| Anti-pattern | What we do instead |
|---|---|
| Shared database across services | Each service owns its data store (MySQL, MongoDB, H2) |
| Synchronous chains of 5+ services | Event-driven via Kafka where possible |
| Distributed monolith (services that must deploy together) | Common-service is a library, but its API is stable |
| Bypassing the gateway | All external traffic enters via gateway-service |

**Service-to-service call chain example (order placement):**

```
Client → gateway-service (rate limit, JWT validation)
       → order-service (idempotency check, persist order)
       → inventory-service (HTTP call: stock check, with circuit breaker)
       → Kafka (publish OrderPlacedEvent)
                ↓
       notification-service (consume, fan out webhooks)
                ↓
       Customer's URL (HMAC-signed POST)
```

---

## 20. Error Handling

**What it is.** Translating server-side failures into responses clients can act on. The standard wire format is RFC 7807 `application/problem+json`.

**Why it matters.** Inconsistent error shapes across services drift over time. Clients write fragile error-parsing code. RFC 7807 is the IETF standard and Spring 6 has first-class support via `org.springframework.http.ProblemDetail`.

**Where it lives.** [`common-service`](common-service/), auto-applied to every servlet service via auto-configuration.

**Files:**
- [`GlobalExceptionHandler.java`](common-service/src/main/java/com/services/common/error/GlobalExceptionHandler.java) — `@RestControllerAdvice` mapping ~10 exception types
- [`CommonErrorHandlingAutoConfiguration.java`](common-service/src/main/java/com/services/common/error/CommonErrorHandlingAutoConfiguration.java) — registers the handler
- [`ResourceNotFoundException`](common-service/src/main/java/com/services/common/error/exception/ResourceNotFoundException.java) · [`BadRequestException`](common-service/src/main/java/com/services/common/error/exception/BadRequestException.java) · [`ConflictException`](common-service/src/main/java/com/services/common/error/exception/ConflictException.java) — domain exceptions

**The handler covers:**

| Exception | Status | Notes |
|---|---|---|
| `BadRequestException` | 400 | Domain-thrown on invalid semantics |
| `MethodArgumentNotValidException` | 400 | `@Valid` failures — includes per-field errors |
| `ConstraintViolationException` | 400 | `@Validated` on path/query params |
| `MissingServletRequestParameterException` | 400 | Required `@RequestParam` missing |
| `MethodArgumentTypeMismatchException` | 400 | Type coercion failure (e.g. `"abc"` for `Long`) |
| `HttpMessageNotReadableException` | 400 | Malformed JSON body |
| `AccessDeniedException` | 403 | Spring Security |
| `ResourceNotFoundException` | 404 | Domain-thrown |
| `NoHandlerFoundException` | 404 | Unmapped URL |
| `HttpRequestMethodNotSupportedException` | 405 | Wrong verb — includes allowed methods |
| `ConflictException` | 409 | Domain-thrown |
| `DataIntegrityViolationException` | 409 | Unique constraint, FK violation — message is sanitized |
| `Exception` (catch-all) | 500 | Logs full stack, returns generic detail |

**The wire format:**

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
  "traceId": "00-abc...",
  "resourceType": "Product",
  "identifier": "missing"
}
```

`type` (URI), `title`, `status`, `detail`, `instance` are RFC 7807. The `timestamp` and `traceId` are extension fields — useful in support tickets.

**The handler builder:**

```java
private static ProblemDetail problem(HttpStatus status, String typeSlug,
                                     String title, String detail,
                                     HttpServletRequest req) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setType(URI.create(PROBLEM_BASE + typeSlug));
    pd.setTitle(title);
    pd.setDetail(detail);
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("timestamp", OffsetDateTime.now().toString());

    String traceId = req.getHeader("X-B3-TraceId");
    if (traceId == null) traceId = req.getHeader("traceparent");
    if (traceId != null) pd.setProperty("traceId", traceId);
    return pd;
}
```

**Validation errors carry per-field details:**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(),
                              "rejectedValue", fe.getRejectedValue(),
                              "message", fe.getDefaultMessage()))
            .toList();
    ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation-failed",
                               "Validation Failed",
                               "One or more fields failed validation", req);
    pd.setProperty("errors", fieldErrors);
    return pd;
}
```

**Production guardrails captured:**
- Don't echo `Exception.getMessage()` for unhandled exceptions — they often leak SQL, file paths, internal hostnames.
- Log full stack at `WARN`/`ERROR`; client sees only a generic detail.
- Sanitize `DataIntegrityViolationException` — its raw message reveals schema (table/column names).
- Set `Content-Type: application/problem+json` so RFC 7807 tooling recognises it.

**How it activates in any service** — just depend on common-service. The auto-configuration registers the handler:

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(RestControllerAdvice.class)
public class CommonErrorHandlingAutoConfiguration {

    @Bean @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
```

A service can override by declaring its own `@Bean GlobalExceptionHandler`.

**How to verify with the existing tests:**

```bash
mvn -pl product-service -am test -Dtest=ProductControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
# 8 slice tests; 3 of them assert specific RFC 7807 responses:
#   - create_withInvalidBody_returns400_problemDetail
#   - getById_whenMissing_returns404_problemDetail
#   - list_withSizeAboveCap_returns400
```

---

## How these concepts compose

The patterns aren't independent — they layer:

```
External request
      ↓
   gateway-service
      ├── #18 API Gateway (routing)
      ├── #15 API Versioning (Path predicate /api/v1/...)
      ├── #9 Rate Limiting (Redis token bucket)
      ├── #5/#6/#7/#8 OAuth2 TokenRelay (JWT forwarding)
      ↓
   resource service (e.g. product-service)
      ├── #5/#6/#7 Bearer JWT validation (resource-server pattern)
      ├── #20 Error handling (RFC 7807 if anything below blows up)
      ├── #1 Endpoint match (/api/v1/products)
      ├── #2 HTTP method dispatch
      ├── #3 Request DTO validation (@Valid)
      ├── #13 Idempotency-Key check (if applicable)
      ├── #11 Pagination binding (PageQuery → Pageable)
      ├── #12 Cache hit? → return; else continue
      ↓
   service layer
      ├── #10 Throttling (Bulkhead/CircuitBreaker around outbound calls)
      ├── repository / DB / RPC
      ↓
   #14 Webhooks (async event fan-out via Kafka)
      ↓
   #4 Status code (chosen by GlobalExceptionHandler if errored, by controller if successful)
      ↓
   #16 OpenAPI describes all the above for clients
```

Every concept on the list has a place in the request lifecycle. Reading the table above + this doc top-to-bottom, you should be able to point at any single line of incoming-request handling and say which concept governs it.

---

## Appendix: Resilience4j — the next learning step

**Status.** Partially used today and worth expanding deliberately. [`order-service`](order-service/) already wires four Resilience4j primitives on its outbound call to inventory-service (see [§10 Throttling](#10-throttling)). The remaining feature — **Rate Limiter** — and a more disciplined per-downstream policy are out of scope for this project but are the natural next step.

**The full Resilience4j toolbox:**

| Feature | What it adds | Already in this repo? |
|---|---|---|
| **Circuit Breaker** | After N failures to a downstream, fail-fast for a cooldown window so callers don't keep racking up timeouts on a service that's clearly down. | Yes — `@CircuitBreaker(name="inventory")` on [`OrderController.placeOrder`](order-service/src/main/java/com/services/order/controller/OrderController.java) |
| **Time Limiter** | Cap call duration; abort hung downstreams so a stuck socket doesn't tie up a thread forever. | Yes — `@TimeLimiter(name="inventory")` |
| **Bulkhead** | Cap concurrent in-flight calls per downstream so one slow service can't starve all your worker threads (cascading failure prevention). | Yes — `@Bulkhead(name="inventory", maxConcurrentCalls=20)` |
| **Retry with backoff** | Smarter retry policy than the load balancer's flat retry — exponential delay with jitter, configurable on which exceptions to retry. | Yes — `@Retry(name="inventory")`; current config is flat `wait-duration=5s`, no jitter |
| **Rate Limiter** | Cap calls per second to a downstream you don't want to overwhelm (different from gateway-side rate limiting in §9 — this is *outbound* protection of a fragile dependency). | **No** — would be added as `@RateLimiter(name="...")` |

**How the existing four already compose** (from §10): annotations evaluate outside-in, so Bulkhead rejects first → CircuitBreaker fail-fasts → TimeLimiter aborts a slow call → Retry kicks in on transient failures. Adding `@RateLimiter` would slot in as the outermost cap on call rate.

**What "the next step" looks like concretely:**

1. **Add `@RateLimiter`** to outbound calls that hit fragile or rate-limited third parties (e.g. webhook delivery in notification-service, where customer endpoints may have their own quotas).
2. **Improve retry policy** — switch from flat `wait-duration` to `IntervalFunction.ofExponentialRandomBackoff(...)` so retries don't synchronise across replicas.
3. **Per-downstream policies** — today `inventory` is the only configured instance; as more services are introduced, each gets its own tuning.
4. **Wire metrics** — Resilience4j ships Micrometer integration. Expose circuit-breaker state and bulkhead saturation to Prometheus/Grafana so you can see *why* a request was rejected.
5. **Consider gateway-side equivalents** — Spring Cloud Gateway's `CircuitBreaker` filter (already used on the `protected-articles` route, see §18) is the gateway-level analog; decide which concerns live at the edge vs at each service.

**Reference:** https://resilience4j.readme.io/docs
