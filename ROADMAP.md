# Roadmap — items not yet implemented

This iteration delivered 5 of the 14 outstanding API topics deeply (see [API_PATTERNS.md](API_PATTERNS.md)).
This file is the **executable backlog** for the rest. Each item lists scope, where it fits, and
a step-by-step plan you can hand to an engineer (or to me in a follow-up session).

Ordering is by impact-per-effort, descending.

---

## 1. Caching — #12

**Goal:** Cache hot reads at the service layer so we don't hit MongoDB / MySQL for every request.

**Scope:** product-service `getById` and `findAll`. Don't cache writes — invalidate on them.

**Where:**
- New `RedisConfig` in `product-service` (Redis is already in the docker-compose for the gateway's session store).
- Annotate `ProductService.getById` with `@Cacheable("products")`.
- Annotate `ProductService.create` / `replace` / `patch` / `delete` with `@CacheEvict("products")` (or `@CachePut` for the put variants).
- Add `spring-boot-starter-data-redis` to product-service.
- Set `spring.cache.type=redis` and `spring.cache.redis.time-to-live=10m` in `application.yaml`.
- Add `@EnableCaching` on the application class.

**Tests:**
- Unit: `ProductService` test with `@SpyBean` on the repository, verify second call doesn't hit the repo.
- Integration: Testcontainers Redis + Mongo, verify cache hit/miss metrics on actuator.

**Industry notes:**
- Always set a TTL — caches without TTL are bug factories.
- Cache the DTO, not the entity, so JPA/Mongo lifecycle never reaches a deserialized cached object.
- Add `Cache-Control` and `ETag` headers on the HTTP layer too — that's *HTTP caching*, distinct from the application cache. Both layers help.

**Effort:** 2–3 hours.

---

## 2. Idempotency — #13

**Goal:** Repeated POSTs with the same `Idempotency-Key` header return the original response, not a duplicate resource.

**Scope:** order-service `POST /api/v1/orders` (the natural place — duplicate orders are expensive). Generalizable to other writes.

**Pattern:** Stripe-style idempotency.

1. Client sends `Idempotency-Key: <UUID>` header.
2. Server hashes `(method, path, idempotency-key)` → key.
3. First call: process, store `(key → response, status, body)` in Redis with TTL ≥ 24h.
4. Subsequent calls: detect existing key, return the stored response without re-processing.
5. Reject if the key is reused for a *different* request body — return `HTTP 422 Unprocessable Entity`.

**Where:**
- New `IdempotencyFilter` in `common-service` (servlet filter, depends on Redis).
- Annotation `@Idempotent` to mark methods/controllers that should opt-in.
- Apply on `OrderController.placeOrder`.

**Tests:**
- Hit the same `POST` twice with same key → same response.
- Hit with same key but different body → 422.
- Hit with no key → standard processing (no idempotency).
- Test concurrent duplicate requests (two requests with same key arriving simultaneously) — second should wait or return the first's response, not double-process.

**Industry notes:**
- Idempotency keys belong on **POST**, **PATCH**, **DELETE** (anything non-idempotent at the HTTP-method level). PUT is idempotent by definition.
- The window matters: 24 hours is typical for Stripe-class APIs. Make TTL configurable.
- Don't store idempotency state in the same DB as your business data — Redis is faster and the TTL semantics are native.

**Effort:** 4–6 hours including tests.

---

## 3. Webhooks — #14

**Goal:** When an order is placed, fire a signed HTTP POST to a customer-supplied URL.

**Scope:** New `webhook` endpoints under notification-service.

**Pattern:**

1. `POST /api/v1/webhooks` — register a webhook subscription `(eventType, callbackUrl, secret)`.
2. When an event happens (e.g. `OrderPlacedEvent` published over Kafka), notification-service consumes it, looks up matching subscriptions, and HTTP-POSTs the event to each `callbackUrl`.
3. Sign the body with HMAC-SHA256 using the subscription secret. Send signature in `X-Webhook-Signature` header.
4. Retry with exponential backoff (10s, 30s, 2m, 10m, 1h, 6h) on non-2xx responses.
5. After max retries, mark the delivery dead and log to a DLQ.

**Where:**
- New module `webhook-service` *or* extension of `notification-service`. Recommend the latter — it already consumes Kafka.
- New tables: `webhook_subscriptions`, `webhook_deliveries`.
- HTTP client: `RestClient` (Spring 6) with Resilience4j retry.
- HMAC signing utility in `common-service`.

**Tests:**
- Stub the customer endpoint with WireMock, fire an event, assert the POST happened with correct signature.
- Test retry behavior: stub 500 first, then 200; assert total delivery attempts.
- Test signature verification independently in `common-service`.

**Industry notes:**
- Always sign webhook payloads. Customers MUST verify the signature before trusting the body.
- Replay protection: include a timestamp in the signed payload, reject deliveries older than 5 minutes.
- Idempotency on customer side: include a unique `event_id`. Customer's handler should be idempotent against this.

**Effort:** 1–2 days for a production-quality implementation.

---

## 4. Apply patterns to remaining controllers (order, inventory, resource-server)

**Goal:** Spread the 5 new patterns from product-service to the rest. Mostly mechanical.

**Per-module checklist:**

```text
☐ Add <dependency>com.services:common-service</dependency>
☐ Replace ad-hoc List<> returns with PagedResponse<T>
☐ Add @Valid on @RequestBody and validation annotations on DTOs
☐ Replace ResponseEntity hand-construction with throw new ResourceNotFoundException(...)
☐ Add @Operation / @ApiResponse OpenAPI annotations
☐ Make sure @RequestMapping is /api/v1/<resource>
☐ Add Resilience4j @CircuitBreaker / @TimeLimiter on outbound calls (#10)
☐ Write @WebMvcTest slice tests covering each method
```

**Effort per module:** ~3–4 hours.

---

## 5. Throttling polish — #10

**Goal:** Round out the throttling layer with bulkheads and TimeLimiters.

**Scope:** Apply `@Bulkhead` (Resilience4j) to each `@FeignClient` and `RestClient` call.

**Why:** A single misbehaving downstream can otherwise saturate caller thread pools. Bulkheads cap concurrency per downstream so failures stay isolated.

**Where:**
- `application.yaml` resilience4j config:

  ```yaml
  resilience4j:
    bulkhead:
      instances:
        product-service:
          maxConcurrentCalls: 50
          maxWaitDuration: 1s
        inventory-service:
          maxConcurrentCalls: 20
  ```

- `@Bulkhead(name = "product-service")` on each method that calls product-service.

**Effort:** 1–2 hours.

---

## 6. REST vs GraphQL — #17 (documentation, not implementation)

This codebase is REST. Adding GraphQL would be a major architectural change. The trade-offs in *this codebase's* context:

| Dimension | REST (current) | GraphQL |
|---|---|---|
| Mobile bandwidth | Multiple round trips, over-fetching | Single round trip, exact fields |
| Caching | Trivial — HTTP layer + Redis on URLs | Hard — POST bodies vary, need persisted-queries to cache |
| Versioning | URL versioning works | Schema evolution + deprecations; harder to reason about |
| Tooling | OpenAPI, Swagger UI, generated clients in 30+ langs | GraphiQL, codegen — newer ecosystem |
| Auth | Mature: Bearer, scopes, per-endpoint | Need field-level authorization at the resolver layer — easy to leak data |
| Performance debugging | One request → one trace | One request → N resolver calls; needs DataLoader to avoid N+1 |
| Microservices fit | Each service owns its REST surface | Federated GraphQL (Apollo Federation) is complex to operate |

**Recommendation for *this* project:** stay on REST. The codebase has 13 services with mixed data stores. Adding GraphQL adds operational complexity (federation gateway, schema registry, observability of resolvers) without clear payoff for what's currently a mostly-server-to-server architecture.

**When to revisit:** if a mobile/web product team starts complaining about over-fetching or N+1 round trips, prototype an Apollo Federation gateway over 2–3 services. If it survives a quarter, commit.

---

## 7. Fix pre-existing skeleton tests in non-product modules

**Why:** Many modules (`netflix-service`, `config-server`, `oauth2-client`) have `contextLoads` tests that were written against Spring Boot 3.1 / Spring AI snapshot APIs. Some now fail because of dep drift. They're skeleton tests with no real assertion value.

**Two paths:**
- **Replace each `contextLoads` with @WebMvcTest slice tests** of the actual controllers (preferred).
- **Or** tag them all with `@EnabledIfSystemProperty(named = "integration.tests", matches = "true")` so they don't run in unit-test phase.

**Specific fixes needed:**
- `netflix-service`: missing `RestClient.Builder` bean in test context. Spring AI 1.x replaced 0.8 snapshots; needs a config update.
- `config-server`: was failing because of Spring Cloud version incompatibility. Should be fixed by this iteration's parent pom bump (verify on next run).
- `oauth2-client`: similar context-load oddities; was already skeletal.

**Effort:** 1 hour per module.

---

## 8. Testcontainers + Colima ergonomics

**Goal:** Make Testcontainers integration tests work out-of-the-box on Colima without `-Dintegration.tests=true`.

**Approach (one of):**

1. **Symlink approach** (machine-specific, sudo required):

   ```bash
   sudo ln -s ~/.colima/default/docker.sock /var/run/docker.sock
   ```

2. **Project-local config** that propagates Colima detection:

   - Create `.mvn/jvm.config` containing `-Ddocker.host=unix:///...`. This sets the system property on every Maven JVM in the project.
   - Or check in a `~/.testcontainers.properties` template at the repo root with instructions in `CONTRIBUTING.md`.

3. **Document the manual setup** clearly in `TESTING.md` (already partially done).

**Effort:** 30 min for option 1, 2 hours for option 2 with proper testing.

---

## 9. CI pipeline (separate from this list, but related)

The repo has a `.github/workflows/maven.yml`. Verify it still passes after the version bump:

- Runs `mvn -B verify` on push.
- Spins up Docker via `services: docker:dind`.
- Sets `DOCKER_HOST=tcp://docker:2375`.

Any failure here is the first thing to look at when reopening the project.

**Effort:** 30 min to verify.

---

## Suggested order for follow-up sessions

1. **Session A** (3 hours): Item 4 (apply patterns to order/inventory/resource-server) + item 7 (fix skeleton tests). Brings the whole codebase to the new baseline.

2. **Session B** (3 hours): Items 1 (caching) + 5 (throttling polish). Adds two more cross-cutting concerns to the same patterns.

3. **Session C** (1 day): Item 2 (idempotency). Real engineering, not boilerplate.

4. **Session D** (1–2 days): Item 3 (webhooks). The biggest standalone feature.

5. **Session E** (any time): Items 6, 8, 9 — docs and infra polish.

By the end of Session B, the codebase is at "production-quality reference architecture" level. Sessions C and D are the long tail.
