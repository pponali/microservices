# LLD: Concurrency Patterns

A practitioner's reference for the 20 concurrency primitives every backend engineer should know — pattern, when to reach for it, idiomatic Java, and where it lives (or naturally fits) in this microservices project.

This doc is intentionally code-first. Read top-to-bottom for the mental model; jump to a section when you need the exact snippet.

## Index

| # | Pattern | Mental model |
|---|---|---|
| 1 | [Concurrency Overview](#1-concurrency-overview) | Doing many things in overlapping time windows. |
| 2 | [Concurrency vs Parallelism](#2-concurrency-vs-parallelism) | Structure vs execution. |
| 3 | [Processes vs Threads](#3-processes-vs-threads) | Isolation vs sharing. |
| 4 | [Thread Lifecycle](#4-thread-lifecycle) | NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED. |
| 5 | [Race Conditions](#5-race-conditions) | Outcome depends on interleaving. |
| 6 | [Mutex](#6-mutex) | One thread holds the door. |
| 7 | [Semaphores](#7-semaphores) | N permits, gate concurrency. |
| 8 | [Condition Variables](#8-condition-variables) | Wait until a predicate becomes true. |
| 9 | [Coarse vs Fine Locking](#9-coarse-vs-fine-locking) | One big lock vs many small ones. |
| 10 | [Reentrant Locks](#10-reentrant-locks) | Same thread can re-acquire. |
| 11 | [Try-Lock](#11-try-lock) | Don't block — fail fast or back off. |
| 12 | [CAS](#12-cas-compare-and-swap) | Lock-free atomic update. |
| 13 | [Deadlock](#13-deadlock) | A→B and B→A waiting on each other forever. |
| 14 | [Livelock](#14-livelock) | Threads "yield" politely and starve. |
| 15 | [Signaling Pattern](#15-signaling-pattern) | One-shot release: latch / barrier / future. |
| 16 | [Thread Pool](#16-thread-pool) | Bounded reusable workers. |
| 17 | [Producer-Consumer](#17-producer-consumer) | Decouple rate via a queue. |
| 18 | [Reader-Writer](#18-reader-writer) | Many readers, exclusive writer. |
| 19 | [Thread-safe Cache](#19-thread-safe-cache) | Concurrent map + atomic upsert. |
| 20 | [Blocking Queue](#20-blocking-queue) | Hand-off + back-pressure in one. |

A note on Java's modern toolbox: prefer **`java.util.concurrent`** over raw `synchronized` and `Thread` whenever you can. The JDK already implements every pattern below correctly. The interview-favourite "implement a mutex with `wait/notify`" almost never beats `ReentrantLock` in production code.

---

## 1. Concurrency Overview

Concurrency is the **composition** of independently-executing tasks — they overlap in time, but may or may not literally run at the same instant. A single CPU running 1,000 connection handlers via `epoll` is concurrent but not parallel. A multi-core server processing 16 requests simultaneously is both.

The job of a concurrency primitive is to coordinate **shared mutable state** between those overlapping tasks without producing wrong answers (race conditions, deadlocks, lost updates).

**In this project**: every Spring Boot service is concurrent by default. Tomcat hands each HTTP request to a worker thread from a pool of 200 by default. State that's reachable from a controller (`@Service` beans, static fields, caches) is multi-threaded territory.

---

## 2. Concurrency vs Parallelism

| | Concurrency | Parallelism |
|---|---|---|
| Definition | **Structure**: multiple tasks make progress in overlapping time | **Execution**: multiple tasks literally run at the same instant |
| Hardware | 1 CPU is enough | Needs ≥2 cores |
| Example | Async HTTP server on 1 vCPU | Stream `parallelStream()` on a 16-core box |
| Goal | Throughput on I/O-bound work | Throughput on CPU-bound work |

Rule of thumb:
- **I/O-bound** (DB calls, HTTP, disk) → high concurrency, small CPU pool, lots of waiting threads. Reach for `@Async`, virtual threads, reactive.
- **CPU-bound** (image resize, matrix multiply) → parallelism, pool sized to `Runtime.getRuntime().availableProcessors()`.

**In this project**: gateway-service and apigateway use Spring WebFlux (Netty event loop = high concurrency, low parallelism). Business services use blocking Tomcat (high concurrency via thread pool).

---

## 3. Processes vs Threads

|  | Process | Thread |
|---|---|---|
| Memory | Own heap, own stack | Shared heap, own stack |
| Creation cost | High (fork/exec, copy-on-write) | Low |
| Isolation | OS-enforced | None — segfault in one thread crashes the JVM |
| Communication | IPC (sockets, pipes, shared memory) | Direct field access |
| Scaling unit | k8s replicas (`replicas: 2` in [helm/microservices/values.yaml](helm/microservices/values.yaml)) | Tomcat worker threads |

When to fork a process: failure isolation (a worker crashing must not kill the supervisor) or language boundary (Python ML model from a Java service). When to spawn a thread: shared in-memory state and microsecond-scale coordination.

```java
// Process boundary — k8s pod replicas of the same Spring Boot jar
// helm/microservices/values.yaml: user-service has replicas: 2
// → two JVMs, two heaps, coordinated externally via Eureka + load balancer.

// Thread boundary — inside one JVM
ExecutorService pool = Executors.newFixedThreadPool(8);
pool.submit(() -> doWork());
```

---

## 4. Thread Lifecycle

```
       new Thread()                start()
NEW ─────────────────────► RUNNABLE ◄──┐
                              │        │ scheduler
                              │        │
                ┌─────────────┼────────┴──────────────┐
                │             │                       │
            wait()/        Lock acquired           sleep(ms)/
            join()         vs blocked              wait(ms)
                │             │                       │
                ▼             ▼                       ▼
            WAITING       BLOCKED               TIMED_WAITING
                │             │                       │
                └─────────────┴───────────────────────┘
                              │
                              ▼ run() returns / exception
                          TERMINATED
```

You inspect this live with:
```java
Thread.State s = thread.getState();   // RUNNABLE, WAITING, ...
```

In a thread dump (`jstack <pid>` or `kubectl exec ... -- jstack 1`) every thread has a state. Most should be `WAITING` or `TIMED_WAITING` (idle pool workers); chronic `BLOCKED` is your tell for lock contention.

**In this project**: `kubectl -n microservices exec deploy/user-service -- jstack 1 | grep "tomcat"` shows you the 200 Tomcat workers, mostly parked on `WAITING`.

---

## 5. Race Conditions

The bug behind half of all "it works on my laptop" stories. Two threads read-modify-write the same memory; the final value is whichever finished last. Classic counter:

```java
class BrokenCounter {
    private int n = 0;
    public void inc() { n++; }   // read, +1, write — three steps
    public int get() { return n; }
}

// 100 threads × 1,000 inc() calls. Expected: 100,000. Actual: somewhere south of 100k.
```

Three fixes, in order of preference:

```java
// 1. AtomicInteger — fastest for single-variable updates (CAS, see #12)
private final AtomicInteger n = new AtomicInteger();
public void inc() { n.incrementAndGet(); }

// 2. synchronized — readable, JIT-optimised, biased locking
public synchronized void inc() { n++; }

// 3. ReentrantLock — when you need tryLock / fair / multiple conditions
private final ReentrantLock lock = new ReentrantLock();
public void inc() { lock.lock(); try { n++; } finally { lock.unlock(); } }
```

**In this project**: a real race lurks in any in-memory cache without a wrapper — see [common-service/src/main/java/com/services/common/idempotency/RedisIdempotencyStore.java](common-service/src/main/java/com/services/common/idempotency/RedisIdempotencyStore.java). It uses Redis `SETNX` to make idempotent inserts atomic across replicas — exactly the multi-process equivalent of `AtomicInteger.compareAndSet`.

---

## 6. Mutex

One thread at a time. The "M" stands for **mut**ual **ex**clusion. In Java, the mutex is built into every `Object` (intrinsic lock) and exposed via `synchronized`, or as a first-class object via `ReentrantLock`.

```java
// Intrinsic — locks `this`
public synchronized BigDecimal debit(long accountId, BigDecimal amount) {
    BigDecimal bal = balances.get(accountId);
    if (bal.compareTo(amount) < 0) throw new InsufficientFunds();
    balances.put(accountId, bal.subtract(amount));
    return bal.subtract(amount);
}

// Explicit — pick your own scope
private final ReentrantLock lock = new ReentrantLock();
public BigDecimal debit(long accountId, BigDecimal amount) {
    lock.lock();
    try { /* same as above */ }
    finally { lock.unlock(); }
}
```

**Where in this project**: order placement (`order-service`) is a natural fit — read inventory, debit it, write the order. Today the integrity is delegated to MySQL transactions (`@Transactional` + `SELECT ... FOR UPDATE`), which is the database's mutex. That's the right answer for most CRUD: prefer DB-level locks over JVM-level locks, since the DB lock survives a pod restart.

---

## 7. Semaphores

A counting mutex. Hand out N "permits"; a thread `acquire()`s one before entering the section and `release()`s when done. If all N are taken, callers block.

```java
// Cap concurrent calls to a flaky downstream at 10
private final Semaphore slots = new Semaphore(10);

public Response call(Req r) throws InterruptedException {
    slots.acquire();
    try { return downstream.invoke(r); }
    finally { slots.release(); }
}
```

This is the **bulkhead** pattern — preventing a slow downstream from soaking every Tomcat thread and starving healthy traffic.

**In this project**: Resilience4j's `Bulkhead` is exactly this — a semaphore wrapped in a Spring annotation. See the rate-limiting docs in [API_PATTERNS.md](API_PATTERNS.md#10-throttling) and [notification-service/src/main/java/com/services/notification/webhook/WebhookHttpClient.java](notification-service/src/main/java/com/services/notification/webhook/WebhookHttpClient.java) which uses a Resilience4j `RateLimiter` (a token-bucket variant of a semaphore).

---

## 8. Condition Variables

"Wait until X is true." A condition variable is paired with a lock: you hold the lock, check a predicate, and if it's not satisfied, you `await()` (which atomically releases the lock + parks the thread). Another thread that mutates the state calls `signal()` / `signalAll()` to wake the waiters.

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notEmpty = lock.newCondition();
private final Deque<Item> q = new ArrayDeque<>();

public Item take() throws InterruptedException {
    lock.lock();
    try {
        while (q.isEmpty()) notEmpty.await();    // releases lock + parks
        return q.removeFirst();                  // re-acquires lock on wake
    } finally { lock.unlock(); }
}

public void put(Item i) {
    lock.lock();
    try {
        q.addLast(i);
        notEmpty.signal();
    } finally { lock.unlock(); }
}
```

The `while` (not `if`) is **load-bearing**: spurious wakeups happen, and another thread may have drained the queue between your wake and your check. Always re-test the predicate.

You almost never write this in 2026 — `BlockingQueue` (#20), `CompletableFuture`, or reactive operators do it for you.

---

## 9. Coarse vs Fine Locking

**Coarse**: one lock for the whole data structure. Simple, hard to deadlock, kills throughput.

```java
public synchronized void transfer(long from, long to, long amount) {
    accounts.get(from).debit(amount);
    accounts.get(to).credit(amount);
}
```

**Fine**: per-row locks. More throughput, more deadlock surface area, ordering matters.

```java
public void transfer(long from, long to, long amount) {
    Account a = accounts.get(from);
    Account b = accounts.get(to);
    Account first  = a.id() < b.id() ? a : b;     // total order on lock IDs
    Account second = a.id() < b.id() ? b : a;     // prevents A→B / B→A deadlock
    synchronized (first) {
        synchronized (second) {
            a.debit(amount);
            b.credit(amount);
        }
    }
}
```

The acquire-in-canonical-order trick (lowest ID first) eliminates the cycle in #13.

**Rule of thumb**: start coarse. Profile. Move to fine only when contention shows up in flame graphs.

**In this project**: MySQL row locks (acquired via `SELECT ... FOR UPDATE`) are fine-grained without the deadlock baggage — InnoDB's lock manager handles ordering. That's why most services here delegate locking to the DB.

---

## 10. Reentrant Locks

A lock that can be re-acquired by the **same thread** without deadlocking itself. Java's `synchronized` is reentrant; `ReentrantLock` is too (the name is the giveaway).

Why this matters: recursive code, or one synchronized method calling another on the same object.

```java
class Cache {
    public synchronized Value get(Key k) {
        Value v = map.get(k);
        return v != null ? v : load(k);
    }
    public synchronized Value load(Key k) {  // re-enters the lock — fine
        Value v = source.fetch(k);
        map.put(k, v);
        return v;
    }
}
```

A non-reentrant lock would deadlock on the second `synchronized`. (Pure `Semaphore(1)` is non-reentrant — using a semaphore as a "mutex" is a common gotcha.)

---

## 11. Try-Lock

"Acquire if free, otherwise tell me — I'll back off or do something else." Crucial for keeping latency tails short.

```java
// Block forever — last resort
lock.lock();

// Bounded wait — timeout into a fallback path
if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
    try { criticalSection(); }
    finally { lock.unlock(); }
} else {
    metrics.lockTimeout.inc();
    return Response.status(503).build();
}

// No wait — best-effort
if (lock.tryLock()) {
    try { piggybackOnAnotherThread(); }
    finally { lock.unlock(); }
}
```

The pattern shines in **leader election** and **dedup**: the first thread that wins the lock does the expensive thing (cache rebuild, schema migration); the rest fall through.

**In this project**: a distributed `tryLock` is what `RedisIdempotencyStore.putIfAbsent` does at the API layer — see [common-service/src/main/java/com/services/common/idempotency/RedisIdempotencyStore.java](common-service/src/main/java/com/services/common/idempotency/RedisIdempotencyStore.java).

---

## 12. CAS (Compare-And-Swap)

The hardware primitive most lock-free code is built on. "Update X to V only if X is currently equal to E; otherwise do nothing and tell me it failed." Implemented in `java.util.concurrent.atomic.*`.

```java
AtomicInteger inflight = new AtomicInteger(0);

// Increment with bounded retry — lock-free
int v;
do {
    v = inflight.get();
    if (v >= MAX) return Response.status(429).build();
} while (!inflight.compareAndSet(v, v + 1));
try {
    return process();
} finally {
    inflight.decrementAndGet();
}
```

Why CAS over a lock?
- **No context switches** — the thread spins in user space.
- **No deadlock** — there's no held resource to deadlock on.
- **Wait-free reads** — `get()` is just a volatile load.

When NOT to use CAS: long critical sections, or when you'd retry forever under contention. Locks are simpler when the work itself is the bottleneck.

**In this project**: every `AtomicLong` micrometer counter — request rate, error count — is built on CAS. Database optimistic locking (`@Version` in JPA) is the persistence-layer analog: `UPDATE ... WHERE id=? AND version=?`.

---

## 13. Deadlock

Four conditions, all required (Coffman):
1. **Mutual exclusion** — at least one resource is non-shareable.
2. **Hold and wait** — thread holds A while waiting for B.
3. **No preemption** — only the holder can release.
4. **Circular wait** — A→B→C→A.

Break any one and you're safe. The cheapest in code is **#4** — acquire locks in a globally consistent order (see #9). The next is **#2** — `tryLock` with timeout + release-and-retry.

```java
// Bad — random order, deadlock at scale
synchronized (a) { synchronized (b) { ... } }   // thread 1
synchronized (b) { synchronized (a) { ... } }   // thread 2 — boom

// Good — canonical order
Object first  = System.identityHashCode(a) < System.identityHashCode(b) ? a : b;
Object second = first == a ? b : a;
synchronized (first)  { synchronized (second) { ... } }
```

**Detect in production**: `kill -3 <pid>` (or `jstack`) prints `Found one Java-level deadlock:` if any threads are deadlocked.

---

## 14. Livelock

The polite cousin of deadlock. Threads aren't stuck — they keep doing work — but no progress happens. The classic story: two people approaching from opposite directions in a corridor, each stepping aside in the same direction. Forever.

```java
// Both retry on contention; both back off the same amount; both contend again.
while (true) {
    if (lock.tryLock()) {
        try { return doWork(); }
        finally { lock.unlock(); }
    }
    Thread.sleep(BACKOFF_MS);   // identical for every caller
}
```

Fix: **randomized exponential back-off** (jitter):
```java
long backoff = Math.min(MAX_BACKOFF, BASE * (1L << attempt));
Thread.sleep(ThreadLocalRandom.current().nextLong(backoff / 2, backoff));
```

**In this project**: Kafka consumer rebalances and Eureka heartbeats both depend on jitter to avoid thundering herds.

---

## 15. Signaling Pattern

"Tell N waiters to start." Used for fan-out / fan-in.

`CountDownLatch` (one-shot, decrement-to-zero):
```java
CountDownLatch start = new CountDownLatch(1);
CountDownLatch done  = new CountDownLatch(workers);

for (int i = 0; i < workers; i++) {
    pool.submit(() -> {
        start.await();          // all park here
        try { work(); }
        finally { done.countDown(); }
    });
}
start.countDown();              // GO
done.await();                   // wait for all to finish
```

`CyclicBarrier` (re-usable; "wait for everyone, then everyone proceeds"):
```java
CyclicBarrier phase = new CyclicBarrier(workers, () -> log.info("phase complete"));
// inside each worker: phase.await();
```

`CompletableFuture.allOf(...)` (modern, returns a future you can chain):
```java
var futures = ids.stream().map(id -> userClient.fetchAsync(id)).toList();
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

**In this project**: when the api-gateway fans out to multiple downstreams to compose a response, this is the shape — though Spring Cloud Gateway is reactive (Project Reactor's `Mono.zip`), which is the same idea expressed as operators.

---

## 16. Thread Pool

A bounded set of worker threads consuming tasks from a queue. The single most important concurrency primitive in any backend service — uncapped thread creation is how you crash a JVM at 3 AM.

```java
// Spring's wrapper
@Bean(name = "asyncExecutor")
public Executor asyncExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(8);
    ex.setMaxPoolSize(32);
    ex.setQueueCapacity(500);
    ex.setThreadNamePrefix("async-");
    ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    ex.initialize();
    return ex;
}
```

Sizing math:
- **CPU-bound**: `cores + 1`.
- **I/O-bound**: `cores * (1 + waitTime / serviceTime)`. At 80% wait, 8 cores → ~40 threads.

Rejection policy is the part juniors miss. With a full queue:
- `AbortPolicy` (default) — throws → 5xx. Good for fail-fast.
- `CallerRunsPolicy` — backpressure: the caller does the work, slowing producers.
- `DiscardOldestPolicy` — drop head. Only OK for fire-and-forget telemetry.

**In this project**: ✅ already implemented at [common-service/src/main/java/com/services/common/config/AsyncConfiguration.java:18](common-service/src/main/java/com/services/common/config/AsyncConfiguration.java) and consumed via `@Async("asyncExecutor")` in [common-service/src/main/java/com/services/common/service/AsyncService.java:27](common-service/src/main/java/com/services/common/service/AsyncService.java) and [notification-service/src/main/java/com/services/notification/webhook/WebhookDeliveryService.java:51](notification-service/src/main/java/com/services/notification/webhook/WebhookDeliveryService.java).

---

## 17. Producer-Consumer

Decouple producers and consumers via a queue so they can run at independent rates. The queue absorbs short bursts; the consumer paces itself; the producer doesn't block.

```java
BlockingQueue<Order> queue = new LinkedBlockingQueue<>(1_000);

// Producer
queue.put(order);            // blocks if full → backpressure

// Consumer
while (running) {
    Order o = queue.take();  // blocks if empty
    process(o);
}
```

Backpressure semantics depend on the queue:
- `LinkedBlockingQueue(N)` → bounded; `put` blocks producers (good).
- `LinkedBlockingQueue()` → unbounded; producers always succeed, but you'll OOM (bad).
- `ArrayBlockingQueue` → bounded, fixed array, lower overhead.
- `SynchronousQueue` → no buffer; hand-off only (good for `Executors.newCachedThreadPool`).

**In this project**: Kafka is a **distributed** producer-consumer. `order-service` produces `OrderPlaced` events; `notification-service` consumes them via `@KafkaListener`. The "queue" is replicated, durable, and horizontally scaled — the exact same shape as `BlockingQueue`, but spanning processes. See [notification-service/src/main/resources/application.properties](notification-service/src/main/resources/application.properties).

---

## 18. Reader-Writer

Many readers OR one writer — never both. Optimal for read-heavy workloads where mutations are rare.

```java
private final ReadWriteLock rw = new ReentrantReadWriteLock();
private final Map<Key, Value> map = new HashMap<>();

public Value get(Key k) {
    rw.readLock().lock();
    try { return map.get(k); }
    finally { rw.readLock().unlock(); }
}

public void put(Key k, Value v) {
    rw.writeLock().lock();
    try { map.put(k, v); }
    finally { rw.writeLock().unlock(); }
}
```

Caveats:
- Reader-heavy: huge throughput win.
- Writer-heavy: WORSE than a plain mutex (more bookkeeping).
- **Use `StampedLock` for highest throughput** in JDK 8+ — supports optimistic reads (no lock acquisition for the common case).

For a concurrent map, **don't roll your own** — `ConcurrentHashMap` has lock striping built in (#19) and is faster than a `ReadWriteLock` around a `HashMap` for almost every workload.

---

## 19. Thread-safe Cache

The right way: `ConcurrentHashMap` with `computeIfAbsent`. The map handles all the locking; the lambda runs **at most once per key**, even under contention.

```java
private final ConcurrentMap<Key, Value> cache = new ConcurrentHashMap<>();

public Value get(Key k) {
    return cache.computeIfAbsent(k, this::loadFromSource);
}
```

Why this works:
- `computeIfAbsent` holds the per-bin lock for the duration of the lambda. Other threads requesting the same key wait; threads requesting different keys (different bins) don't block.
- The lambda is **not** invoked if the key is already present — no wasted DB call.

Anti-pattern (race condition):
```java
if (!cache.containsKey(k)) cache.put(k, load(k));   // BROKEN — two threads can both load
```

For TTL / size eviction / async refresh, reach for **Caffeine**:
```java
Cache<Key, Value> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(5))
    .build();
```

**In this project**: Redis is the distributed equivalent — `product-service` uses `@Cacheable` (Spring Cache abstraction) backed by Redis. See [product-service/src/main/java/com/services/product/service/ProductService.java](product-service/src/main/java/com/services/product/service/ProductService.java) and [API_PATTERNS.md §12](API_PATTERNS.md#12-caching).

---

## 20. Blocking Queue

The queue at the heart of producer-consumer. The "blocking" verbs (`put` / `take`) couple flow control to capacity — your throttling falls out of the data structure for free.

```java
// Pick the right one for your workload
BlockingQueue<T> bounded   = new LinkedBlockingQueue<>(1000);   // most common
BlockingQueue<T> unbounded = new LinkedBlockingQueue<>();       // beware OOM
BlockingQueue<T> handoff   = new SynchronousQueue<>();          // no buffer, direct hand-off
BlockingQueue<T> priority  = new PriorityBlockingQueue<>();     // ordered by Comparator

// Three appetites for non-blocking variants:
bounded.put(item);                                  // block forever
bounded.offer(item, 100, TimeUnit.MILLISECONDS);    // bounded wait → reject if late
bounded.offer(item);                                // never block, return false if full
```

Pair with a `ThreadPoolExecutor`:

```java
new ThreadPoolExecutor(
    8, 32,                                  // core, max
    60, TimeUnit.SECONDS,                   // keepalive
    new LinkedBlockingQueue<>(500),         // ← THIS is the blocking queue
    new ThreadFactoryBuilder().setNameFormat("worker-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

That single object combines:
- **Thread pool** (#16)
- **Blocking queue** (#20)
- **Producer-consumer** (#17)
- **Backpressure** via the rejection policy

It's the cleanest one-line application of half this list.

---

## How these compose in this project

```
HTTP request                                   Kafka event
    │                                              │
    ▼                                              ▼
Tomcat thread pool         (#16)         @KafkaListener (#17, #20)
    │                                              │
    ▼                                              ▼
@Service method            (#5: race surface)
    │                       (#6: synchronized OR DB FOR UPDATE)
    │                       (#10: reentrant via @Transactional)
    ▼
@Async("asyncExecutor")    (#16: ThreadPoolTaskExecutor in common-service)
    │                       — fire-and-forget side effects
    ▼
Webhook delivery           (#7: RateLimiter — semaphore)
                           (#11: Resilience4j Bulkhead — tryAcquire with timeout)
                           (#14: jittered retry)
```

## Recommended reading order if you're new to JCIP-style concurrency

1. **Start**: §3 (processes vs threads) → §4 (lifecycle) → §5 (race) → §16 (thread pool).
2. **Then**: §6 (mutex) → §10 (reentrant) → §11 (try-lock) → §13 (deadlock).
3. **Lock-free**: §12 (CAS) → §19 (`ConcurrentHashMap`).
4. **Coordination**: §15 (latch) → §17 + §20 (queues).
5. **Tuning**: §9 (granularity) → §18 (reader-writer) → §14 (livelock).

The single best book on this material is *Java Concurrency in Practice* (Goetz et al., 2006). It's twenty years old and 90% of it is still correct.
