package com.services.common.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage abstraction for idempotency records. Redis-backed by default
 * ({@link RedisIdempotencyStore}); swap-in an in-memory implementation for tests.
 */
public interface IdempotencyStore {

    /**
     * Atomically reserve a key. Returns true if this caller is the first
     * to use the key (and may proceed with processing); false if the key
     * is already reserved by a concurrent or earlier request.
     */
    boolean tryReserve(String key, Duration ttl);

    /** Persist the final response for a previously-reserved key. */
    void put(String key, IdempotencyRecord record, Duration ttl);

    /** Look up a stored record. Empty if the key isn't known or has expired. */
    Optional<IdempotencyRecord> get(String key);
}
