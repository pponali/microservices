package com.services.notification.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the webhook signature is reproducible and matches what a customer would
 * compute on their end (so they can verify the signature before trusting the payload).
 */
class WebhookSignerTest {

    @Test
    void sign_producesStripeStyleHeader_withT_andV1() {
        byte[] body = "{\"orderId\":\"abc\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "shh-keep-it-secret";
        long ts = 1_714_723_200L;

        String sig = WebhookSigner.sign(body, secret, ts);

        assertThat(sig).startsWith("t=" + ts + ",v1=");
        // hex SHA-256 = 64 chars
        String hex = sig.substring(("t=" + ts + ",v1=").length());
        assertThat(hex).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void sign_isDeterministic_givenSameInputs() {
        byte[] body = "abc".getBytes();
        String s1 = WebhookSigner.sign(body, "secret", 1L);
        String s2 = WebhookSigner.sign(body, "secret", 1L);
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    void sign_changesWhenAnyInputChanges() {
        byte[] body = "abc".getBytes();
        String base = WebhookSigner.sign(body, "secret", 1L);

        assertThat(WebhookSigner.sign(body, "secret", 2L)).isNotEqualTo(base);          // ts
        assertThat(WebhookSigner.sign("xyz".getBytes(), "secret", 1L)).isNotEqualTo(base); // body
        assertThat(WebhookSigner.sign(body, "secret2", 1L)).isNotEqualTo(base);          // secret
    }

    @Test
    void sign_matches_independentHmacComputation() throws Exception {
        // Independent HMAC-SHA256 computation, exactly as a customer's verification code would do it.
        byte[] body = "{\"x\":1}".getBytes();
        String secret = "k";
        long ts = 100L;

        String signed = ts + "." + new String(body, StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = "t=" + ts + ",v1=" + HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));

        assertThat(WebhookSigner.sign(body, secret, ts)).isEqualTo(expected);
    }
}
