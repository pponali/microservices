package com.services.notification.webhook;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer-registered webhook destination.
 *
 * <p>Each subscription declares which {@code eventType} it cares about, the
 * {@code callbackUrl} to POST events to, and a {@code secret} used to HMAC-sign
 * outgoing payloads. Customers verify signatures on their end before trusting
 * the body.</p>
 */
@Entity
@Table(name = "webhook_subscriptions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Event topic, e.g. "order.placed". Used to filter which subscriptions fire for a given event. */
    private String eventType;

    /** Customer's HTTPS endpoint that will receive the POST. */
    private String callbackUrl;

    /** HMAC-SHA256 secret. Stored at rest as-is for the demo; in production encrypt at rest. */
    private String secret;

    private boolean enabled;

    private Instant createdAt;
}
