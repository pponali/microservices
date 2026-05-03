package com.services.notification.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription response. The {@code secret} is intentionally omitted — it's write-only.
 */
@Schema(description = "Webhook subscription as returned by the API. Secret is never echoed back.")
public record WebhookSubscriptionResponse(
        UUID id,
        String eventType,
        String callbackUrl,
        boolean enabled,
        Instant createdAt
) {
    public static WebhookSubscriptionResponse from(WebhookSubscription sub) {
        return new WebhookSubscriptionResponse(
                sub.getId(),
                sub.getEventType(),
                sub.getCallbackUrl(),
                sub.isEnabled(),
                sub.getCreatedAt()
        );
    }
}
