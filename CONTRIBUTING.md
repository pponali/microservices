# Contributing

## Local environment

| Tool | Version |
|---|---|
| JDK | 21 (we compile with JDK 25's javac, target Java 21 bytecode for Lombok ASM compat) |
| Maven | 3.9.x |
| Docker | any (Docker Desktop, Colima, OrbStack, Rancher Desktop) |
| Redis | 7.x — in docker-compose, but can be run standalone for tests |

Install on macOS:

```bash
brew install openjdk@21 maven
brew install colima docker docker-compose
colima start --cpu 4 --memory 6
```

## Building

```bash
mvn -B -DskipTests clean package    # all 12 modules
mvn -B -pl <module> -am test        # unit + slice tests for a module
```

## Running tests against real infra (Testcontainers)

Many integration tests use [Testcontainers](https://www.testcontainers.org/). They auto-detect Docker via:

1. `DOCKER_HOST` env var
2. `~/.testcontainers.properties` containing `docker.host=...`
3. `/var/run/docker.sock`

By default, **integration tests are tagged `@EnabledIfSystemProperty(named = "integration.tests", matches = "true")`** so a plain `mvn test` does not require Docker. Opt-in:

```bash
mvn -B test -Dintegration.tests=true
```

### Colima users

Colima exposes its socket at `~/.colima/default/docker.sock`. The parent pom's `maven-surefire-plugin` config forwards `DOCKER_HOST` to forked test JVMs, so:

```bash
export DOCKER_HOST=unix:///${HOME}/.colima/default/docker.sock
mvn -B test -Dintegration.tests=true
```

For a permanent setup, write `~/.testcontainers.properties`:

```bash
echo "docker.host=unix:///${HOME}/.colima/default/docker.sock" > ~/.testcontainers.properties
```

### Docker Desktop users

Nothing special — Testcontainers finds the daemon automatically.

### Symlink alternative (one-time setup, requires sudo)

```bash
sudo ln -s ~/.colima/default/docker.sock /var/run/docker.sock
```

After this, no env var needed. Survives Colima restarts (the symlink target is the path, not the inode).

## Pattern reference

When adding new endpoints to **any** module, follow the patterns in [API_PATTERNS.md](API_PATTERNS.md):

- URL versioning: `/api/v1/<resource>` (plural noun)
- Errors: throw exceptions from `com.services.common.error.exception` — never construct `ResponseEntity` for errors yourself
- Pagination: bind `PageQuery` (from common-service) and return `PagedResponse<T>`
- Validation: `@Valid` on `@RequestBody`; Bean Validation annotations on the DTO
- OpenAPI: `@Tag` on the controller, `@Operation` + `@ApiResponses` on each method
- Status codes: 200 (read), 201 + Location (create), 204 (delete), 404 (missing), 400 (bad input), 409 (conflict)

The reference implementation is `ProductController` in product-service. Copy its shape.

## Where common cross-cutting concerns live

| Concern | Location |
|---|---|
| RFC 7807 error responses | `common-service/src/main/java/com/services/common/error/` |
| Pagination DTOs | `common-service/.../pagination/` |
| OpenAPI auto-config | `common-service/.../openapi/` |
| Idempotency-Key filter | `common-service/.../idempotency/` (opt-in via `services.idempotency.enabled=true`) |
| Webhook signing | `notification-service/.../webhook/WebhookSigner` |
| Gateway rate limiter | `gateway-service/.../ratelimit/RateLimiterConfig` |

Adding common-service to a new module:

```xml
<dependency>
    <groupId>com.services</groupId>
    <artifactId>common-service</artifactId>
    <version>${project.version}</version>
</dependency>
```

The auto-configurations register themselves — no `@Import` needed.

## Commit style

Short imperative subject (≤72 chars), blank line, body explaining *why* not *what*. Example:

```
Add Idempotency-Key filter to common-service

Stripe-style idempotency: clients send Idempotency-Key on POST/PUT/PATCH/DELETE,
the filter caches (status, body, headers) in Redis with 24h TTL, replays on
duplicate requests, returns 422 if the key is reused with a different body,
and 409 if a concurrent request holds the reservation. Order-service opts in
for /api/v1/orders POSTs where double-processing matters most.
```

## Commit hygiene

- Don't commit `target/`, `.idea/`, `.DS_Store` — already in .gitignore
- Don't commit credentials. The `application.properties` files have `${REDIS_PASSWORD:}` style env-var references — keep it that way.
- Don't bypass hooks (`--no-verify`) without explicit reason in the commit body.
