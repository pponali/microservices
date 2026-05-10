package com.services.concurrency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pattern #5 (Race Condition) — three counters that are pounded by N threads.
 *
 * Run {@code RaceConditionTest} to see {@link Broken} lose updates while
 * {@link Synchronized} and {@link Atomic} match the expected total exactly.
 */
public final class Counters {

    private Counters() {}

    /** ❌ {@code n++} is read-modify-write — three steps, not one. Lost updates under contention. */
    public static final class Broken {
        private int n = 0;
        public void inc() { n++; }
        public int get() { return n; }
    }

    /** ✅ Intrinsic lock on {@code this}. Fine for low-contention. */
    public static final class Synchronized {
        private int n = 0;
        public synchronized void inc() { n++; }
        public synchronized int get() { return n; }
    }

    /** ✅ Lock-free, fastest single-variable updater. */
    public static final class Atomic {
        private final AtomicInteger n = new AtomicInteger();
        public void inc() { n.incrementAndGet(); }
        public int get() { return n.get(); }
    }

    /** ✅ Explicit ReentrantLock — same correctness, more knobs (tryLock, fair, conditions). */
    public static final class Reentrant {
        private final ReentrantLock lock = new ReentrantLock();
        private int n = 0;
        public void inc() {
            lock.lock();
            try { n++; } finally { lock.unlock(); }
        }
        public int get() {
            lock.lock();
            try { return n; } finally { lock.unlock(); }
        }
    }
}
