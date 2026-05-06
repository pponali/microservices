package com.services.common.idempotency;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the full idempotency state machine using an in-memory store stand-in for Redis.
 *
 * <p>Covers:
 * <ul>
 *   <li>First-time request passes through and stores the response</li>
 *   <li>Replay request returns the cached response without re-invoking the chain</li>
 *   <li>Replay with different body returns 422</li>
 *   <li>Concurrent reservation: second simultaneous request returns 409</li>
 *   <li>GET requests bypass the filter entirely</li>
 *   <li>Failure responses (4xx/5xx) are NOT cached, so the client can retry</li>
 * </ul>
 * </p>
 */
class IdempotencyFilterTest {

    private InMemoryIdempotencyStore store;
    private IdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore();
        filter = new IdempotencyFilter(store);
    }

    @Test
    void firstRequest_passesThroughAndStoresResponse() throws Exception {
        MockHttpServletRequest req = postWithKey("k1", "{\"x\":1}");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = succeedingChain(201, "{\"id\":\"abc\"}");

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(201);
        assertThat(resp.getContentAsString()).isEqualTo("{\"id\":\"abc\"}");
        assertThat(chain.getRequest()).isNotNull(); // chain was actually invoked
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void replayRequest_returnsCachedResponse_withoutInvokingChain() throws Exception {
        // First call to populate the cache
        filter.doFilter(postWithKey("k1", "{\"x\":1}"), new MockHttpServletResponse(),
                succeedingChain(201, "{\"id\":\"abc\"}"));

        // Second call with same key + same body
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(postWithKey("k1", "{\"x\":1}"), resp, chain);

        assertThat(resp.getStatus()).isEqualTo(201);
        assertThat(resp.getContentAsString()).isEqualTo("{\"id\":\"abc\"}");
        assertThat(resp.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(chain.getRequest()).isNull(); // chain was NOT invoked on the replay
    }

    @Test
    void replayWithDifferentBody_returns422() throws Exception {
        // Populate cache with body {"x":1}
        filter.doFilter(postWithKey("k1", "{\"x\":1}"), new MockHttpServletResponse(),
                succeedingChain(201, "{\"id\":\"abc\"}"));

        // Same key, different body
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(postWithKey("k1", "{\"x\":2}"), resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(422);
        assertThat(resp.getContentType()).isEqualTo("application/problem+json");
        assertThat(resp.getContentAsString()).contains("Idempotency Key Reused With Different Body");
    }

    @Test
    void getRequest_bypassesFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        req.addHeader("Idempotency-Key", "k1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = succeedingChain(200, "[]");

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(store.size()).isEqualTo(0); // never persisted
    }

    @Test
    void requestWithoutKey_bypassesFilter() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/orders");
        req.setContent("{\"x\":1}".getBytes());
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = succeedingChain(201, "{}");

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(201);
        assertThat(store.size()).isEqualTo(0); // no key → no cache
    }

    @Test
    void errorResponse_isNotCached_soClientCanRetry() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(postWithKey("k1", "{\"x\":1}"), resp, succeedingChain(500, "boom"));

        assertThat(resp.getStatus()).isEqualTo(500);
        assertThat(store.size()).isEqualTo(0); // 5xx not cached
    }

    @Test
    void concurrentSecondRequest_whileFirstInFlight_returns409() throws Exception {
        // Simulate a winner that has reserved but not yet finished.
        String key = "winner-key";
        // Build the same compound key the filter would: SHA-256(POST|/api/v1/orders|winner-key).
        // We pre-reserve via the store so the filter sees an existing reservation but no record.
        // The filter's compound key is hashed internally; we simulate by intercepting tryReserve.
        store.forceReserveAll(true);

        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(postWithKey(key, "{\"x\":1}"), resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(409);
        assertThat(resp.getContentType()).isEqualTo("application/problem+json");
        assertThat(resp.getContentAsString()).contains("Concurrent Request");
    }

    // ----- helpers --------------------------------------------------------------------

    private static MockHttpServletRequest postWithKey(String key, String body) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/orders");
        req.addHeader("Idempotency-Key", key);
        req.setContent(body.getBytes());
        req.setContentType("application/json");
        return req;
    }

    private static MockFilterChain succeedingChain(int status, String body) {
        return new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest req,
                                   HttpServletResponse resp) throws java.io.IOException {
                resp.setStatus(status);
                resp.setContentType("application/json");
                resp.getWriter().write(body);
            }
        });
    }

    /** Minimal in-memory store sufficient to exercise the filter's branches. */
    static class InMemoryIdempotencyStore implements IdempotencyStore {
        private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
        private final Map<String, Boolean> reservations = new HashMap<>();
        private boolean denyAllReservations = false;

        public void forceReserveAll(boolean deny) { this.denyAllReservations = deny; }

        public int size() { return records.size(); }

        @Override
        public boolean tryReserve(String key, Duration ttl) {
            if (denyAllReservations) return false;
            return reservations.putIfAbsent(key, true) == null;
        }

        @Override
        public void put(String key, IdempotencyRecord record, Duration ttl) {
            records.put(key, record);
            reservations.remove(key);
        }

        @Override
        public Optional<IdempotencyRecord> get(String key) {
            return Optional.ofNullable(records.get(key));
        }
    }
}
