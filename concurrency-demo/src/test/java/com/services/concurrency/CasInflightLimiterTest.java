package com.services.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pattern #12 — CAS-based bounded counter. The invariant under load:
 * {@code accepted + rejected = total} AND {@code accepted ≤ max}.
 */
class CasInflightLimiterTest {

    @Test
    void neverAdmitsMoreThanMaxConcurrentSlots() throws Exception {
        int max = 10;
        CasInflightLimiter limiter = new CasInflightLimiter(max);

        int threads = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger peak     = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (limiter.tryEnter()) {
                        try {
                            accepted.incrementAndGet();
                            // Update the running peak observed inside the critical section.
                            peak.updateAndGet(p -> Math.max(p, limiter.currentInflight()));
                            // Simulate brief work so concurrent slots actually overlap.
                            Thread.sleep(5);
                        } finally {
                            limiter.leave();
                        }
                    } else {
                        rejected.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(15, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads, accepted.get() + rejected.get(),
                "every thread should be either accepted or rejected");
        assertTrue(peak.get() <= max,
                "peak inflight " + peak.get() + " exceeded max " + max);
        assertEquals(0, limiter.currentInflight(),
                "inflight should drain back to zero");
    }
}
