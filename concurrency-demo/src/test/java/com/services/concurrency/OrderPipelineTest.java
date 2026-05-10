package com.services.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Patterns #17 (Producer-Consumer) + #20 (Blocking Queue).
 *
 * <p>4 producers × 250 orders each = 1,000 orders flow through a queue of
 * capacity 100 to 2 consumers. Asserts every order is processed exactly once
 * and that the queue capacity actually applies backpressure.
 */
class OrderPipelineTest {

    @Test
    void everyEnqueuedOrderIsProcessedExactlyOnce() throws Exception {
        OrderPipeline pipe = new OrderPipeline(/* capacity */ 100);

        int producers = 4;
        int perProducer = 250;
        int total = producers * perProducer;

        ExecutorService consumers = Executors.newFixedThreadPool(2);
        consumers.submit(pipe.consumer());
        consumers.submit(pipe.consumer());

        ExecutorService producersPool = Executors.newFixedThreadPool(producers);
        CountDownLatch produced = new CountDownLatch(producers);
        for (int p = 0; p < producers; p++) {
            int prodId = p;
            producersPool.submit(() -> {
                try {
                    for (int i = 0; i < perProducer; i++) {
                        pipe.enqueue(new OrderPipeline.Order(prodId * 1000L + i, "item-" + i));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    produced.countDown();
                }
            });
        }
        assertTrue(produced.await(10, TimeUnit.SECONDS));
        producersPool.shutdownNow();

        // Wait for queue to drain
        for (int i = 0; i < 100 && pipe.size() > 0; i++) Thread.sleep(50);
        pipe.shutdown();
        consumers.shutdown();
        assertTrue(consumers.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(total, pipe.processed().size(),
                "consumer should have processed every produced order");

        // No duplicates
        long distinct = pipe.processed().stream().map(OrderPipeline.Order::id).distinct().count();
        assertEquals(total, distinct, "ids must be unique");
    }

    @Test
    void offerWithTimeout_returnsFalseWhenQueueIsFull() throws Exception {
        // Capacity 1, no consumer running → queue fills immediately.
        OrderPipeline pipe = new OrderPipeline(1);
        assertTrue(pipe.offer(new OrderPipeline.Order(1, "x"), 100));
        assertFalse(pipe.offer(new OrderPipeline.Order(2, "y"), 50),
                "second offer should time out — backpressure works");
    }
}
