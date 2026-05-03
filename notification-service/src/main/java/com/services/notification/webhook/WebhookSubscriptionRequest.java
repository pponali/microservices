package com.services.notification.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Register a webhook subscription")
public class WebhookSubscriptionRequest {

    @NotBlank
    @Pattern(regexp = "[a-z]+\\.[a-z.]+",
            message = "eventType must be lowercase dotted, e.g. 'order.placed'")
    @Schema(example = "order.placed")
    private String eventType;

    @NotBlank
    @Pattern(regexp = "https?://.+",
            message = "callbackUrl must start with http:// or https://")
    @Schema(example = "https://customer.example.com/webhooks/orders")
    private String callbackUrl;

    @NotBlank
    @Size(min = 16, max = 256, message = "secret must be 16-256 chars")
    @Schema(description = "HMAC secret. Treat as sensitive.", example = "a-strong-random-string")
    private String secret;
}
