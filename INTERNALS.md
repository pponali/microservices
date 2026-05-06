# How It Works — Internals & Algorithms

A deep-dive into how this project actually performs client-side and
server-side load balancing. Read this top-to-bottom and you'll understand
every box in the architecture diagram and why the curls in
[TESTING.md](TESTING.md) produce the output they do.

---

## 1. The 30-second mental model

| Layer | Component | Where it lives | Purpose |
|---|---|---|---|
| **Discovery** | Eureka server | `discovery-service` (port 8761) | A phone book: maps service names → list of IP:port |
| **LB engine** | Spring Cloud LoadBalancer | Inside every *caller* JVM | Picks one IP:port from the list per request |
| **HTTP client (declarative)** | OpenFeign | `catalog-service` | Interface-as-client; LB-aware automatically |
| **HTTP client (imperative)** | `@LoadBalanced` RestTemplate | `catalog-service` | Plain RestTemplate + an interceptor that does the LB rewrite |
| **Server-side LB** | Spring Cloud Gateway | `apigateway` (port 8080) | Edge proxy that uses the same LB engine on behalf of external clients |

Three things to internalize:

1. **Eureka is just a phone book.** It does not proxy traffic. It does not pick instances. It only stores `(service-name → [IP:port, IP:port, ...])` and lets clients ask for the list.
2. **The load balancer is inside the caller**, not on a server. Each calling service has the relevant phone-book pages cached locally and runs the picking algorithm itself.
3. **`http://user-service/...` and `lb://user-service` are not real URLs.** They're sentinels that an interceptor recognises and rewrites into a real `http://172.18.0.3:8081/...` before the request leaves the JVM.

---

## 2. The full request lifecycle

Trace one call: `curl http://localhost:8082/catalog/with-user-feign`

```
                                       ┌──────────────────────────────────────────────┐
                                       │ catalog-service JVM (172.18.0.4)             │
                                       │                                              │
                                       │  CatalogController.viaFeign()                │
                                       │     │                                        │
                                       │     ▼                                        │
                                       │  userFeignClient.whoami()                    │
                                       │     │                                        │
                                       │     ▼                                        │
                                       │  Spring Cloud LoadBalancer                   │
                                       │  ┌────────────────────────────────────────┐  │
                                       │  │ 1. ServiceInstanceListSupplier         │  │
                                       │  │    (cached from Eureka)                │  │
                                       │  │    user-service → [172.18.0.3:8081,    │  │
   ┌─────────┐                         │  │                   172.18.0.6:8081]     │  │
   │ curl    │ HTTP                    │  │                                        │  │
   │ on host │─────────────▶  :8082    │  │ 2. RoundRobinLoadBalancer              │  │
   └─────────┘                         │  │    AtomicInteger counter               │  │
                                       │  │    pick = counter++ % size             │  │
                                       │  │    returns 172.18.0.3:8081             │  │
                                       │  └────────────────────────────────────────┘  │
                                       │     │                                        │
                                       │     ▼                                        │
                                       │  Feign rewrites URL:                         │
                                       │    http://user-service/users/whoami          │
                                       │    →  http://172.18.0.3:8081/users/whoami   │
                                       │     │                                        │
                                       └─────┼────────────────────────────────────────┘
                                             │ HTTP GET
                                             ▼
                                       ┌─────────────────────────────────────────────┐
                                       │ user-service-2 (172.18.0.3:8081)            │
                                       │ returns {"host":"a3791ac081bb",...}         │
                                       └─────────────────────────────────────────────┘
```

**Key insight:** Eureka is **not consulted on this request path.** The instance
list was fetched ahead of time and cached. The picking happens in-memory in
catalog-service. The whole call adds maybe 200 microseconds of LB overhead.

---

## 3. The two cache layers inside the caller

Inside `catalog-service` (and inside `api-gateway`, same story) there are
**two** in-memory caches of the registry:

```
                ┌─────────────────────────────────────┐
                │ Eureka server (source of truth)     │
                │ in-memory map of all registrations  │
                └────────────────┬────────────────────┘
                                 │ HTTP poll every 30s
                                 │ (eureka.client.registry-fetch-interval-seconds)
                                 ▼
   ┌───────────────────────────────────────────────────────────┐
   │ catalog-service JVM                                       │
   │                                                           │
   │  ┌─────────────────────────────────────────────────────┐  │
   │  │ L1: Eureka client cache                             │  │
   │  │   Full registry: every service Eureka knows about   │  │
   │  │   {api-gateway:[...], catalog-service:[...],        │  │
   │  │    user-service:[172.18.0.3, 172.18.0.6]}           │  │
   │  └────────────────────────┬────────────────────────────┘  │
   │                           │ ServiceInstanceListSupplier   │
   │                           │ reads from here               │
   │                           ▼                               │
   │  ┌─────────────────────────────────────────────────────┐  │
   │  │ L2: LoadBalancer's per-service cache                │  │
   │  │   Just user-service: [172.18.0.3, 172.18.0.6]       │  │
   │  │   TTL ~35s (CachingServiceInstanceListSupplier)     │  │
   │  └─────────────────────────────────────────────────────┘  │
   │                           ▲                               │
   │                           │ each call to                  │
   │                           │ RoundRobinLoadBalancer reads  │
   │                           │ this list                     │
   └───────────────────────────────────────────────────────────┘
```

**Why two layers?** L1 is generic (the whole registry). L2 is specialised
per service and lets the LB add instance-list filters (e.g. health-only,
zone-affinity) without rebuilding from scratch every request.

**Refresh cadence (defaults):**

| Cache | Refresh trigger | Default interval |
|---|---|---|
| L1 (Eureka client) | Periodic poll | 30s |
| L2 (LB cache) | TTL expiry | ~35s |
| Eureka registry | Every heartbeat | 30s receive, 90s expiry |

---

## 4. The round-robin algorithm

The default `LoadBalancerClient` picks via `RoundRobinLoadBalancer`. The
relevant logic, distilled:

```java
public class RoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    // start at a random offset so multiple JVMs don't all land on the same
    // instance for their first request (would create a thundering herd)
    final AtomicInteger position = new AtomicInteger(new Random().nextInt(1000));

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return supplier.get(request).next().map(instances -> {
            int pos = Math.abs(position.incrementAndGet());
            ServiceInstance pick = instances.get(pos % instances.size());
            return new DefaultResponse(pick);
        });
    }
}
```

That `% instances.size()` is exactly why the curl loops show clean ABABAB
alternation across 6 calls when there are 2 instances:

```
counter=N → call 1 → N+1 % 2 = 1 → instances[1]
counter=N → call 2 → N+2 % 2 = 0 → instances[0]
counter=N → call 3 → N+3 % 2 = 1 → instances[1]
...
```

### Other algorithms you can swap in

| Algorithm | When to use |
|---|---|
| `RoundRobinLoadBalancer` (default) | Even traffic, equally-capable instances |
| `RandomLoadBalancer` | Same as above; less predictable, simpler |
| Weighted round-robin | Instances have different capacity (e.g. mixed VM sizes) |
| Response-time-weighted | Auto-prefer faster instances; reactive to slow nodes |
| Zone-aware | Prefer instances in the same AZ for latency |

You change the algorithm by providing a configuration class:

```java
@Configuration
public class CustomLbConfig {
    @Bean
    ReactorLoadBalancer<ServiceInstance> randomLB(Environment env, LoadBalancerClientFactory factory) {
        String name = env.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(factory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
}
```

Then attach it to a specific service with
`@LoadBalancerClient(name = "user-service", configuration = CustomLbConfig.class)`.

---

## 5. How the URL rewrite actually works

The "magic" of `http://user-service/...` and `lb://user-service` is one
interceptor per HTTP-client style. They're different objects but they do the
same thing.

### 5a. `@LoadBalanced` RestTemplate

[RestClientConfig.java](catalog-service/src/main/java/com/catalogservice/config/RestClientConfig.java)
declares the bean:

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

`@LoadBalanced` is a qualifier that triggers
`LoadBalancerAutoConfiguration` to inject a `LoadBalancerInterceptor` into
*this specific* RestTemplate's interceptor chain.

Per-request flow:

1. `restTemplate.getForObject("http://user-service/users/whoami", Map.class)`
2. RestTemplate sends the request through its interceptor chain.
3. `LoadBalancerInterceptor` intercepts it. Sees host = `user-service`.
4. Asks `LoadBalancerClient.choose("user-service")` → gets `172.18.0.3:8081`.
5. Reconstructs the URI: `http://172.18.0.3:8081/users/whoami`.
6. Lets the request proceed with the rewritten URI.

Without `@LoadBalanced` on the bean, none of this happens — `user-service`
would be treated as a literal hostname and DNS would fail.

### 5b. OpenFeign

[UserFeignClient.java](catalog-service/src/main/java/com/catalogservice/client/UserFeignClient.java):

```java
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/users/whoami")
    Map<String, Object> whoami();
}
```

At startup, Spring scans for `@FeignClient`, generates a proxy
implementation, and wires `BlockingLoadBalancerClient` into the proxy's
HTTP-client chain (Feign supports plug-in clients: OkHttp, Apache HC, JDK
HttpClient — all the same way).

Per-request flow:

1. `userFeignClient.whoami()` invokes the generated proxy.
2. The proxy builds a request with target `http://user-service/users/whoami`.
3. The Feign LB integration intercepts, asks `LoadBalancerClient.choose("user-service")` → gets `172.18.0.3:8081`.
4. Rewrites the URI, sends the HTTP call.

**Note:** It's the *same* `LoadBalancerClient` used by the RestTemplate path.
Same cache, same algorithm, same instance list. Only the HTTP client
plumbing differs. That's the whole point of running them side-by-side in
this demo — the two endpoints rotate identically because the LB layer is
shared.

### 5c. Spring Cloud Gateway (`lb://`)

[apigateway/.../application.yaml](apigateway/src/main/resources/application.yaml):

```yaml
routes:
  - id: user-service
    uri: lb://user-service
    predicates: [ Path=/api/users/** ]
    filters: [ StripPrefix=1 ]
```

The gateway has its own LB-aware route filter chain. When a request matches
this route:

1. Gateway sees the route URI scheme is `lb` (not `http` or `https`).
2. `ReactiveLoadBalancerClientFilter` kicks in.
3. Asks `ReactiveLoadBalancer.choose("user-service")` → gets `172.18.0.3:8081`.
4. Rewrites the destination URI in the exchange.
5. The downstream gateway proxy filter sends the request to the real instance.

To external curl, this is **server-side load balancing**: caller hits
`localhost:8080`, has no idea who answered. Internally it's the same
`spring-cloud-loadbalancer` library doing the picking — so really, "client
vs server" is about *who runs the LB*, not *what algorithm runs*.

---

## 6. Service registration: how Eureka learns about instances

Each service's `application.yaml` configures it as a Eureka client:

```yaml
spring:
  application:
    name: user-service
eureka:
  client:
    service-url:
      defaultZone: http://discovery:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

**Lifecycle inside an instance:**

| Event | What it does |
|---|---|
| Instance starts | Bootstrap pulls Eureka client lib (transitive from `spring-cloud-starter-netflix-eureka-client`) |
| `ApplicationReady` event | Eureka client POSTs registration to `http://discovery:8761/eureka/apps/USER-SERVICE` with `{instanceId, ipAddr, port, status: UP}` |
| Every 30s thereafter | PUT heartbeat to `/eureka/apps/USER-SERVICE/<instance-id>` (the "lease renewal") |
| Graceful shutdown | DELETE `/eureka/apps/USER-SERVICE/<instance-id>` (cancellation) |
| Crash / network loss | No DELETE happens. Server eviction kicks in (next section). |

**Why two `user-service` containers register as separate instances** even
though they have identical config: each container gets a different Docker IP
(`172.18.0.3` vs `172.18.0.6`) and `${random.value}` produces a different
`instance-id`. Eureka deduplicates by `instance-id`, so two register cleanly.

---

## 7. Failure detection & resilience

This is where most of the operational subtlety lives.

### The eviction protocol

```
user-service-1 ──30s heartbeat──▶ Eureka
                (lease-renewal-interval-in-seconds)

If Eureka has not received a heartbeat for 90s
(lease-expiration-duration-in-seconds), the lease is considered expired
and the instance is removed from the registry.
```

Defaults are deliberately conservative for production. **In dev they feel
slow.** You can tighten:

```yaml
# inside each client's application.yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 5     # heartbeat every 5s
    lease-expiration-duration-in-seconds: 10 # evict after 10s of silence
```

### Timeline when an instance dies

For the default config (heartbeat 30s, lease 90s, registry-fetch 30s):

| Time after death | What's happening |
|---|---|
| **0 s** | Container stops. No more heartbeats. |
| **0 – 90 s** | Eureka still has the instance in the registry. Clients still see 2 instances. |
| **0 – 90 s** | Round-robin still picks the dead instance ~50% of the time → connection refused / timeout. |
| **~90 s** | Eureka detects expiry, removes instance from registry. |
| **+ 0–30 s** | Next registry-fetch tick on each client pulls the smaller list. |
| **+ 0–35 s** | LoadBalancer L2 cache TTL expires; rebuilt from L1. |
| **~120-150 s after death** | All clients fully aware. Round-robin only includes the survivor. No more failures. |

So **for up to ~2 minutes** after a hard crash, calls can still hit the
dead instance. Without retry, every other call would fail.

### Built-in retry (Spring Cloud LoadBalancer)

Spring Cloud LB has retry support that "tries the next instance on
failure". To enable in a service that calls others:

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true                              # default true in 2025.0.0
        max-retries-on-same-service-instance: 0
        max-retries-on-next-service-instance: 1    # fall over to a different instance once
        retryable-status-codes: 500,502,503,504
```

With this, the failure path becomes:

1. RoundRobin picks the dead instance.
2. Feign / RestTemplate attempts the call → connection refused.
3. LB-retry catches the exception → picks the *next* instance from the list.
4. Call succeeds.
5. Caller never sees the failure.

This is enough for instance-level resilience during the eviction window.

### Resilience4j (out of scope for this project, but the next learning step)

For richer resilience layer Resilience4j on top:

| Feature | What it adds |
|---|---|
| **Circuit breaker** | After N failures to a service, fail-fast for a cooldown window (don't keep racking up timeouts) |
| **Time limiter** | Cap call duration; abort hung downstreams |
| **Bulkhead** | Cap concurrent calls per downstream so one slow service can't starve all your threads |
| **Retry with backoff** | Smarter retry policy (jitter, exponential delay) than LB's flat retry |
| **Rate limiter** | Cap calls per second to a downstream you don't want to overwhelm |

Wiring is one annotation per Feign method, e.g.
`@CircuitBreaker(name = "user-service", fallbackMethod = "fallback")`.

### Eureka self-preservation (a footgun)

Eureka has a **self-preservation mode**: if it loses heartbeats from too
many instances at once (default >15% in 1 minute), it stops evicting. The
reasoning is "if half my registry is 'dead' suddenly, the network is more
likely broken than the instances are."

In small dev clusters this can fire on legitimate single-instance death
because 1 of 2 = 50%. You'll see a yellow banner on the Eureka dashboard.
Disable for dev:

```yaml
# discovery-service application.yaml
eureka:
  server:
    enable-self-preservation: false
```

This project leaves it on default (production-safe). If your resilience
demo seems sluggish, this is likely why.

---

## 8. Network topology (Docker Compose)

```
                    Docker bridge network: microservices_default
                    (172.18.0.0/16)
                    ┌──────────────────────────────────────────────────┐
                    │                                                  │
                    │  discovery        172.18.0.2  :8761              │
                    │  user-service-1   172.18.0.3  :8081              │
                    │  catalog-service  172.18.0.4  :8082              │
                    │  api-gateway      172.18.0.5  :8080              │
                    │  user-service-2   172.18.0.6  :8081              │
                    │                                                  │
                    │  All services resolve "discovery" via            │
                    │  Docker's embedded DNS → 172.18.0.2              │
                    │                                                  │
                    │  Eureka stores ipAddr (172.18.0.x), not          │
                    │  hostname, because of `prefer-ip-address: true` │
                    └──────────────────────────────────────────────────┘
                                  │
                                  │ Published ports (host ↔ container)
                                  │
        ┌───────────────────┬─────┴──────────────────┬──────────────────────────┐
        │                   │                        │                          │
        ▼                   ▼                        ▼                          ▼
   localhost:8761      localhost:8082        localhost:8080            (no host port)
   (Eureka)            (catalog direct)      (gateway, lb:// targets)  user-service is
                                                                         only reachable
                                                                         from inside the
                                                                         Docker network
```

`user-service` is intentionally **not** published to the host. The whole
point is that external callers reach it only through the gateway (or via
catalog-service) — that's how server-side LB makes sense in this demo.

---

## 9. Why each industry-best-practice setting matters

These are the small things in the configs that aren't obvious but matter:

| Setting | Where | Why |
|---|---|---|
| `spring.application.name: <name>` | every client `application.yaml` | The Eureka registration ID. `lb://<name>` and `@FeignClient(name=<name>)` resolve via this. |
| `prefer-ip-address: true` | every client `application.yaml` | In Docker, container hostnames don't always resolve from outside the container. Registering by IP avoids that. |
| `instance-id: ${app.name}:${random.value}` | every client `application.yaml` | Without this, two replicas with the same hostname would collide in Eureka. |
| `register-with-eureka: false` (server only) | `discovery-service` only | The Eureka server should not register itself as a client of itself. |
| `spring-boot-starter-actuator` | every service | Eureka uses `/actuator/health` as the health-check endpoint when configured to. Also gives you `/actuator/health` for ops. |
| `discovery.locator.enabled: false` | `apigateway/application.yaml` | Explicit routes are clearer for learning. Setting this to `true` would auto-create routes for *every* Eureka service, which is convenient but opaque. |
| `EUREKA_URL` as env var | every Dockerfile/compose | The same image runs locally (`http://localhost:8761/eureka/`) or in Docker (`http://discovery:8761/eureka/`) by overriding the env var. Don't bake the URL into config. |

---

## 10. What you can change to deepen the demo

Things to try once you have it running:

1. **Crank the registry-fetch interval** to 5s and watch resilience recover faster:
   ```yaml
   eureka:
     client:
       registry-fetch-interval-seconds: 5
   ```

2. **Enable LB retry** in `catalog-service` and re-run the resilience demo —
   now 100% of calls succeed during the eviction window.

3. **Add a 3rd `user-service` replica** to `docker-compose.yml` and re-run the
   curl loops. You should now see 3-way rotation (ABCABC).

4. **Swap the algorithm to `RandomLoadBalancer`** by adding a
   `@LoadBalancerClient` configuration in catalog-service. The clean ABAB
   pattern becomes random-looking.

5. **Read `docker logs catalog-service -f`** while running curls. You'll see
   Spring Cloud LB's `DEBUG`-level logs about which instance got picked.
   Set `logging.level.org.springframework.cloud.loadbalancer: DEBUG` in
   `application.yaml` to surface them.

6. **Write a circuit breaker** with Resilience4j around the Feign client.
   Take down both `user-service` instances and watch the breaker open
   (fail-fast) instead of timing out.

---

## TL;DR

- **Eureka** = phone book.
- **Spring Cloud LoadBalancer** = lives in the caller's JVM, picks an instance from a cached list using round-robin.
- **`@LoadBalanced` / `lb://` / `@FeignClient`** = three different sentinels that all trigger the same LB engine.
- **Resilience** = heartbeats + lease expiry + LB-native retry + (optionally) Resilience4j circuit breakers.
- **Server-side vs client-side LB** = same algorithm, different question of *who runs it*. The gateway runs LB on behalf of external clients (server-side); catalog-service runs LB for itself (client-side).
