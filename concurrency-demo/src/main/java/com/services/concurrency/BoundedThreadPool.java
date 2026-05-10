package com.services.concurrency;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pattern #16 (Thread Pool) + the four moving parts behind every
 * {@link ThreadPoolExecutor}:
 * <ol>
 *   <li>core / max worker counts</li>
 *   <li>keep-alive idle timeout for non-core workers</li>
 *   <li>bounded {@link LinkedBlockingQueue} for backpressure</li>
 *   <li>rejection policy that decides what happens when the queue is full
 *       (here: {@code CallerRunsPolicy} — caller does the work itself,
 *       which throttles the producer)</li>
 * </ol>
 */
public final class BoundedThreadPool {

    private BoundedThreadPool() {}

    /** Convenience factory matching {@code common-service.AsyncConfiguration}. */
    public static ThreadPoolExecutor newDefault(String namePrefix) {
        return new ThreadPoolExecutor(
                /* core */         8,
                /* max  */         32,
                /* keepalive */    60, TimeUnit.SECONDS,
                /* queue */        new LinkedBlockingQueue<>(500),
                /* factory */      namedFactory(namePrefix),
                /* rejection */    new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static ThreadFactory namedFactory(String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, prefix + "-" + seq.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
    }
}
