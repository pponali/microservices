package com.services.concurrency;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Pattern #17 (Producer-Consumer) + #20 (Blocking Queue).
 *
 * <p>The classic shape: a {@link BlockingQueue} in the middle, producers on one
 * side, consumers on the other. The queue capacity provides natural backpressure —
 * when full, {@code put()} blocks the producer; when empty, {@code take()} blocks
 * the consumer.
 *
 * <p>This demo accumulates processed orders into a thread-safe list so a test
 * can assert that nothing was dropped.
 */
public class OrderPipeline {

    public record Order(long id, String item) {}

    private final BlockingQueue<Order> queue;
    private final List<Order> processed = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    public OrderPipeline(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /** Producer-side hook. Blocks if the queue is full → backpressure. */
    public void enqueue(Order o) throws InterruptedException {
        queue.put(o);
    }

    /** Try-enqueue with a timeout — caller decides the fallback (drop, retry, 503...). */
    public boolean offer(Order o, long timeoutMs) throws InterruptedException {
        return queue.offer(o, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Consumer loop — runs in its own thread, pulls one order at a time,
     * exits cleanly on {@link #shutdown()} after draining.
     */
    public Runnable consumer() {
        return () -> {
            while (running || !queue.isEmpty()) {
                try {
                    Order o = queue.poll(50, TimeUnit.MILLISECONDS);
                    if (o != null) {
                        processed.add(o);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };
    }

    public void shutdown() { running = false; }

    public int size() { return queue.size(); }

    public List<Order> processed() { return List.copyOf(processed); }
}
