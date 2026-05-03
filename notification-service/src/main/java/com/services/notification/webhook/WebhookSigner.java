package com.services.notification.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 signer for webhook payloads.
 *
 * <p>Output format: {@code t=<unix_ts>,v1=<hex_signature>} — Stripe-style. The receiver
 * verifies by re-computing HMAC-SHA256 over {@code "<unix_ts>.<raw_body>"} with the same secret
 * and constant-time-comparing to {@code v1}.</p>
 *
 * <p>Including the timestamp in the signed string prevents replay attacks: the receiver
 * rejects deliveries older than (e.g.) 5 minutes.</p>
 */
public final class WebhookSigner {

    private WebhookSigner() {}

    /**
     * Build the signature header value.
     *
     * @param body raw request body bytes
     * @param secret subscription secret
     * @param unixTimestamp current epoch seconds (parameter so callers can fix it for testing)
     * @return header value, e.g. {@code "t=1714723200,v1=ab12cd34..."}
     */
    public static String sign(byte[] body, String secret, long unixTimestamp) {
        String signedPayload = unixTimestamp + "." + new String(body, StandardCharsets.UTF_8);
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] mac = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            return "t=" + unixTimestamp + ",v1=" + HexFormat.of().formatHex(mac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }
}
