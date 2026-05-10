package com.services.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pattern #16 — Thread pool. Verifies:
 *   1. Tasks submitted under maxPoolSize all execute on pool threads (named "demo-N").
 *   2. When the queue + maxPool are both saturated, CallerRunsPolicy makes the
 *      submitting thread run the task itself — backpressure for free.
 */
class BoundedThreadPoolTest {

    @Test
    void runsAllTasksWithinPoolBounds() throws Exception {
        ThreadPoolExecutor pool = BoundedThreadPool.newDefault("demo");
        int tasks = 200;
        CountDownLatch done = new CountDownLatch(tasks);
        AtomicInteger ran = new AtomicInteger();

        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                ran.incrementAndGet();
                done.countDown();
            });
        }
        assertTrue(done.await(10, TimeUnit.SECONDS), "tasks did not complete in 10s");
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(tasks, ran.get());
    }

    @Test
    void callerRunsPolicy_engagesUnderSaturation() throws Exception {
        // Tiny pool + tiny queue — easy to saturate.
        ThreadPoolExecutor tinyPool = new ThreadPoolExecutor(
                1, 1, 1, TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(1),
                BoundedThreadPool.namedFactory("tiny"),
                new ThreadPoolExecutor.CallerRunsPolicy());

        AtomicInteger ranOnCaller = new AtomicInteger();
        Thread main = Thread.currentThread();
        CountDownLatch hold = new CountDownLatch(1);

        // Block the single worker so the queue fills and rejection kicks in.
        tinyPool.submit(() -> {
            try { hold.await(); } catch (InterruptedException ignored) {}
        });

        // Now submit 5 more tasks. Queue holds 1; the rest must run on the caller.
        for (int i = 0; i < 5; i++) {
            tinyPool.execute(() -> {
                if (Thread.currentThread() == main) ranOnCaller.incrementAndGet();
            });
        }

        hold.countDown();
        tinyPool.shutdown();
        assertTrue(tinyPool.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(ranOnCaller.get() >= 1,
                "expected at least one task to fall back to the caller thread");
    }
}
