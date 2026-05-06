package com.services.notification.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Delivers webhook events to subscriber URLs with HMAC-SHA256 signing and exponential-backoff retry.
 *
 * <h3>Retry policy</h3>
 * <p>Schedule: 10s, 30s, 2m, 10m, 1h, 6h. After 6 attempts, give up and log to ERROR
 * (which a real system would feed into a DLQ for ops review).</p>
 *
 * <p>This is a minimal viable implementation. Production hardening would add:
 * <ul>
 *   <li>Persistent delivery records ({@code webhook_deliveries} table) with status per attempt</li>
 *   <li>A separate worker pool to drive retries (not blocking caller threads)</li>
 *   <li>Circuit breaker per destination so a permanently-broken endpoint doesn't burn retries forever</li>
 *   <li>Customer-facing dashboard showing recent delivery attempts + failures</li>
 * </ul>
 * Items 2-3 are tracked in ROADMAP.md as the natural next step.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeliveryService {

    private static final long[] BACKOFF_SECONDS = {10, 30, 120, 600, 3600, 21600};

    private final WebhookSubscriptionRepository subscriptions;
    private final ObjectMapper objectMapper;

    /**
     * Fire all matching subscriptions for the given event type.
     *
     * <p>Delivery is async (annotated {@code @Async}) so the caller — typically a Kafka consumer
     * — doesn't block on slow customer endpoints.</p>
     */
    @Async
    public void publish(String eventType, Object payload) {
        var matches = subscriptions.findByEventTypeAndEnabledTrue(eventType);
        if (matches.isEmpty()) {
            log.debug("No active subscribers for event {}", eventType);
            return;
        }

        byte[] body;
        try {
            // Wrap so customers always see the same envelope regardless of source.
            Map<String, Object> envelope = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "type", eventType,
                    "createdAt", Instant.now().toString(),
                    "data", payload);
            body = objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook event {}", eventType, e);
            return;
        }

        for (WebhookSubscription sub : matches) {
            deliverWithRetry(sub, body);
        }
    }

    private void deliverWithRetry(WebhookSubscription sub, byte[] body) {
        for (int attempt = 0; attempt < BACKOFF_SECONDS.length; attempt++) {
            if (attemptDelivery(sub, body, attempt + 1)) {
                return;
            }
            if (attempt < BACKOFF_SECONDS.length - 1) {
                try {
                    TimeUnit.SECONDS.sleep(BACKOFF_SECONDS[attempt]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Webhook delivery interrupted for sub {}", sub.getId());
                    return;
                }
            }
        }
        log.error("Webhook delivery to {} failed after {} attempts — would DLQ in production",
                sub.getCallbackUrl(), BACKOFF_SECONDS.length);
    }

    private boolean attemptDelivery(WebhookSubscription sub, byte[] body, int attempt) {
        long ts = Instant.now().getEpochSecond();
        String signature = WebhookSigner.sign(body, sub.getSecret(), ts);

        try {
            HttpStatusCode status = RestClient.create()
                    .post()
                    .uri(sub.getCallbackUrl())
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", sub.getEventType())
                    .header("X-Webhook-Attempt", String.valueOf(attempt))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();

            if (status.is2xxSuccessful()) {
                log.info("Webhook delivered to {} on attempt {} ({})", sub.getCallbackUrl(), attempt, status.value());
                return true;
            }
            log.warn("Webhook to {} returned {} on attempt {}", sub.getCallbackUrl(), status.value(), attempt);
            return false;
        } catch (RestClientResponseException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            // Permanent errors (4xx except 408 and 429) shouldn't be retried.
            if (statusCode.is4xxClientError() && statusCode.value() != 408 && statusCode.value() != 429) {
                log.warn("Webhook to {} returned permanent error {}; not retrying",
                        sub.getCallbackUrl(), statusCode);
                return true; // don't retry, but treat as terminal
            }
            log.warn("Webhook to {} attempt {} failed with {}", sub.getCallbackUrl(), attempt, statusCode);
            return false;
        } catch (Exception e) {
            log.warn("Webhook to {} attempt {} threw {}", sub.getCallbackUrl(), attempt, e.toString());
            return false;
        }
    }
}
