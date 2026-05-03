package com.services.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stripe-style idempotency. Activated by an {@code Idempotency-Key} request header.
 *
 * <h3>Protocol</h3>
 * <ol>
 *   <li>Client sends {@code POST/PUT/PATCH/DELETE} with {@code Idempotency-Key: <UUID>}.</li>
 *   <li>Filter computes {@code key = SHA-256(method | path | idempotency-key)}.</li>
 *   <li>If a record exists for {@code key}:
 *     <ul>
 *       <li>Body hash matches → replay stored {@code (status, headers, body)} as the response.</li>
 *       <li>Body hash differs → return 422 (key reuse with different body).</li>
 *     </ul>
 *   </li>
 *   <li>Otherwise: process normally. On success (2xx/3xx), store the response with 24h TTL.</li>
 * </ol>
 *
 * <h3>Concurrency</h3>
 * <p>Two simultaneous requests with the same key would both pass the "look up record" step
 * and both process. We mitigate that with {@link IdempotencyStore#tryReserve(String, Duration)}
 * via Redis SETNX. The loser waits briefly and re-checks for a stored record. If still
 * absent (the winner is still processing), we return 409 Conflict — the client should retry.</p>
 *
 * <p>Concurrency handling is conservative on purpose: the cost of returning 409 is one
 * client retry; the cost of double-processing is real money for non-idempotent operations.</p>
 *
 * <h3>What's NOT cached</h3>
 * <ul>
 *   <li>Non-mutating methods (GET/HEAD/OPTIONS) — they're idempotent at the HTTP level.</li>
 *   <li>4xx/5xx responses on the first call — we want the client to retry on transient failures.</li>
 * </ul>
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String HEADER = "Idempotency-Key";
    private static final Set<String> APPLICABLE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);

    private final IdempotencyStore store;

    public IdempotencyFilter(IdempotencyStore store) {
        this.store = store;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank() || !APPLICABLE_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        // Force the body to be read so we can hash it. Spring will dispatch the cached copy
        // to the controller so the consumer still sees the body.
        wrappedRequest.getInputStream().readAllBytes();
        String bodyHash = sha256(wrappedRequest.getContentAsByteArray());

        String compoundKey = sha256(
                request.getMethod() + "|" + request.getRequestURI() + "|" + idempotencyKey);

        Optional<IdempotencyRecord> existing = store.get(compoundKey);
        if (existing.isPresent()) {
            replay(existing.get(), bodyHash, response);
            return;
        }

        // Try to reserve; if reservation fails, another request is in flight.
        if (!store.tryReserve(compoundKey, RESERVATION_TTL)) {
            // Brief retry — the winner may finish quickly.
            existing = store.get(compoundKey);
            if (existing.isPresent()) {
                replay(existing.get(), bodyHash, response);
                return;
            }
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"https://api.services.com/problems/concurrent-idempotent-request",
                     "title":"Concurrent Request",
                     "status":409,
                     "detail":"Another request with this Idempotency-Key is in progress. Retry shortly."}""");
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            int status = wrappedResponse.getStatus();
            // Read body BEFORE copyBodyToResponse — that call clears the wrapper's buffer.
            byte[] capturedBody = wrappedResponse.getContentAsByteArray();
            Map<String, String> headerMap = new LinkedHashMap<>();
            for (String name : wrappedResponse.getHeaderNames()) {
                String value = wrappedResponse.getHeader(name);
                if (value != null) headerMap.put(name, value);
            }
            wrappedResponse.copyBodyToResponse();

            // Only cache successful 2xx/3xx — don't cache failures.
            if (status >= 200 && status < 400) {
                IdempotencyRecord record = new IdempotencyRecord(bodyHash, status, headerMap, capturedBody);
                store.put(compoundKey, record, TTL);
                log.debug("Stored idempotency record for key {}", idempotencyKey);
            }
        }
    }

    private static void replay(IdempotencyRecord record, String requestBodyHash, HttpServletResponse response)
            throws IOException {

        if (!record.getRequestBodyHash().equals(requestBodyHash)) {
            // Same key, different body → 422
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"https://api.services.com/problems/idempotency-key-mismatch",
                     "title":"Idempotency Key Reused With Different Body",
                     "status":422,
                     "detail":"This Idempotency-Key was previously used with a different request body. Use a fresh key."}""");
            return;
        }

        response.setStatus(record.getStatus());
        Optional.ofNullable(record.getHeaders()).orElse(Collections.emptyMap())
                .forEach(response::setHeader);
        response.setHeader("Idempotent-Replayed", "true");
        if (record.getBody() != null) {
            response.getOutputStream().write(record.getBody());
        }
    }

    private static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
