package com.services.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Key resolvers used by the gateway's {@code RequestRateLimiter} filter.
 *
 * <p>The filter works as a token bucket against Redis:
 * <ul>
 *   <li>Each {@link KeyResolver} returns a string. That string is the bucket key.</li>
 *   <li>Each request decrements the bucket by 1.</li>
 *   <li>Buckets refill at {@code redis-rate-limiter.replenishRate} tokens / second up to {@code burstCapacity}.</li>
 *   <li>If the bucket is empty, the gateway returns {@code 429 Too Many Requests}.</li>
 * </ul>
 * </p>
 *
 * <p>We register two resolvers so different routes can pick the right strategy:</p>
 * <ul>
 *   <li><b>{@code ipKeyResolver}</b> (primary) — limits per remote IP. Good default for anonymous traffic.</li>
 *   <li><b>{@code userKeyResolver}</b> — limits per authenticated principal name. Use for routes behind OAuth2.</li>
 * </ul>
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Per-IP bucket. Falls back to "anonymous" when the remote address is missing
     * (so we never NPE and so the rate-limiter still applies to broken clients).
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(addr -> addr.getAddress().getHostAddress())
                        .orElse("anonymous")
        );
    }

    /**
     * Per-authenticated-user bucket. Reads the OAuth2 principal from the security context
     * and falls back to IP if the request is anonymous.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "user:" + principal.getName())
                .switchIfEmpty(Mono.just("anonymous:" +
                        Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                .map(addr -> addr.getAddress().getHostAddress())
                                .orElse("unknown")));
    }
}
