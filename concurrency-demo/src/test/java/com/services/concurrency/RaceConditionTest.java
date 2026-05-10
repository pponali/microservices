package com.services.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pattern #5 — Race conditions.
 *
 * <p>100 threads × 1,000 increments = 100,000 expected. The unsafe counter
 * almost always returns less; the safe ones return the exact total.
 */
class RaceConditionTest {

    private static final int THREADS = 100;
    private static final int PER_THREAD = 1_000;
    private static final int EXPECTED = THREADS * PER_THREAD;

    @Test
    void brokenCounter_losesUpdates_underContention() throws Exception {
        Counters.Broken c = new Counters.Broken();
        run(c::inc, c::get);
        // We don't assert exact loss (timing-dependent) but assert it's racy:
        // re-run a few times, prove at least one shows < EXPECTED.
        boolean sawLoss = false;
        for (int i = 0; i < 5 && !sawLoss; i++) {
            Counters.Broken cc = new Counters.Broken();
            run(cc::inc, cc::get);
            if (cc.get() < EXPECTED) sawLoss = true;
        }
        assertTrue(sawLoss, "Broken counter should lose updates at least once across 5 runs");
    }

    @Test
    void synchronizedCounter_isExact() throws Exception {
        Counters.Synchronized c = new Counters.Synchronized();
        run(c::inc, c::get);
        assertEquals(EXPECTED, c.get());
    }

    @Test
    void atomicCounter_isExact() throws Exception {
        Counters.Atomic c = new Counters.Atomic();
        run(c::inc, c::get);
        assertEquals(EXPECTED, c.get());
    }

    @Test
    void reentrantLockCounter_isExact() throws Exception {
        Counters.Reentrant c = new Counters.Reentrant();
        run(c::inc, c::get);
        assertEquals(EXPECTED, c.get());
    }

    private void run(Runnable inc, IntSupplier ignored) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < PER_THREAD; j++) inc.run();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "workers did not finish in 30s");
        pool.shutdownNow();
    }

    @SuppressWarnings("unused")
    private static Consumer<Object> noop() { return o -> {}; }
}
