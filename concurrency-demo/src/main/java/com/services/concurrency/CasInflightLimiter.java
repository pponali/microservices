package com.services.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pattern #12 (Compare-And-Swap).
 *
 * <p>A bounded inflight counter implemented entirely without locks.
 * Each {@link #tryEnter()} attempts to {@code compareAndSet} the count from
 * {@code v} to {@code v+1}; if another thread changed the value first, we
 * re-read and retry. {@link #leave()} is a simple decrement.
 *
 * <p>This is the same algorithm Resilience4j {@code Bulkhead} uses internally.
 */
public class CasInflightLimiter {

    private final int max;
    private final AtomicInteger inflight = new AtomicInteger();

    public CasInflightLimiter(int max) {
        if (max <= 0) throw new IllegalArgumentException("max must be positive");
        this.max = max;
    }

    /**
     * @return {@code true} if a slot was reserved; {@code false} if at capacity.
     */
    public boolean tryEnter() {
        for (;;) {
            int v = inflight.get();
            if (v >= max) return false;
            if (inflight.compareAndSet(v, v + 1)) return true;
            // CAS lost a race — another thread incremented. Retry from current value.
        }
    }

    public void leave() {
        int v = inflight.decrementAndGet();
        if (v < 0) {
            // Defensive: someone called leave() without a matching tryEnter().
            inflight.incrementAndGet();
            throw new IllegalStateException("leave() without tryEnter()");
        }
    }

    public int currentInflight() { return inflight.get(); }

    public int max() { return max; }
}
