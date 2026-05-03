package com.services.common.idempotency;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation. Uses {@code SETNX} (via {@code setIfAbsent}) for the
 * reserve step so two concurrent requests with the same key can never both proceed.
 *
 * <p>Keys are stored under {@code idem:<hash>} so they don't collide with other Redis usage.</p>
 */
public class RedisIdempotencyStore implements IdempotencyStore {

    private static final String PREFIX = "idem:";
    private static final String RESERVATION_PREFIX = "idem-lock:";
    // Sentinel value used when a key is reserved but the response hasn't been computed yet.
    private static final String IN_FLIGHT_SENTINEL = "__in_flight__";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisIdempotencyStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryReserve(String key, Duration ttl) {
        Boolean reserved = redisTemplate.opsForValue()
                .setIfAbsent(RESERVATION_PREFIX + key, IN_FLIGHT_SENTINEL, ttl);
        return Boolean.TRUE.equals(reserved);
    }

    @Override
    public void put(String key, IdempotencyRecord record, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + key, record, ttl);
        // The reservation is no longer needed; the cached record is the source of truth.
        redisTemplate.delete(RESERVATION_PREFIX + key);
    }

    @Override
    public Optional<IdempotencyRecord> get(String key) {
        Object value = redisTemplate.opsForValue().get(PREFIX + key);
        if (value instanceof IdempotencyRecord r) {
            return Optional.of(r);
        }
        return Optional.empty();
    }
}
