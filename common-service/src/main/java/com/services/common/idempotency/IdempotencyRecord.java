package com.services.common.idempotency;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * The cached response for a previously-seen Idempotency-Key.
 *
 * <p>Stored in Redis under a key derived from {@code (method, path, idempotency-key)}
 * with a 24-hour TTL. On a repeat request with the same key:
 * <ul>
 *   <li>If {@code requestBodyHash} matches → replay the stored response.</li>
 *   <li>If {@code requestBodyHash} differs → return HTTP 422 (key reuse with different body).</li>
 * </ul>
 * </p>
 *
 * <p>Implements Serializable so Spring's Redis serializer can round-trip it.</p>
 */
public class IdempotencyRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String requestBodyHash;
    private int status;
    private Map<String, String> headers;
    private byte[] body;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String requestBodyHash, int status, Map<String, String> headers, byte[] body) {
        this.requestBodyHash = requestBodyHash;
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public String getRequestBodyHash() { return requestBodyHash; }
    public void setRequestBodyHash(String requestBodyHash) { this.requestBodyHash = requestBodyHash; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
}
