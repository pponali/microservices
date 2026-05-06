package com.services.notification.webhook;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Outbound webhook HTTP call, isolated in its own bean so Spring AOP can apply
 * {@code @RateLimiter} to it. Self-invoked private methods bypass proxies, so the
 * annotation must live on a public method of a separately injected bean.
 *
 * <p>The {@code webhook-delivery} rate limiter caps how fast we hammer subscriber
 * endpoints across all subscriptions — a deliberately conservative default to avoid
 * tripping customer-side rate limits during a burst of order events. When the limit
 * is exceeded, Resilience4j throws {@link io.github.resilience4j.ratelimiter.RequestNotPermitted}
 * which the caller treats as a transient failure (retry on next backoff tick).</p>
 */
@Component
@Slf4j
public class WebhookHttpClient {

    private final RestClient restClient = RestClient.create();

    @RateLimiter(name = "webhook-delivery")
    public HttpStatusCode post(String url, byte[] body, String signature, String eventType, int attempt) {
        return restClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header("X-Webhook-Signature", signature)
                .header("X-Webhook-Event", eventType)
                .header("X-Webhook-Attempt", String.valueOf(attempt))
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();
    }
}
