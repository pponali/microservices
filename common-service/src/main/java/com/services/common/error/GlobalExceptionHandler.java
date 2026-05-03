package com.services.common.error;

import com.services.common.error.exception.BadRequestException;
import com.services.common.error.exception.ConflictException;
import com.services.common.error.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project-wide exception handler. Produces RFC 7807 {@code application/problem+json} responses
 * (Spring's {@link ProblemDetail}) so every error from every service has a consistent shape.
 *
 * <p>Each handler answers: <em>what HTTP status, what type URI, what title, what extra fields</em>.
 * Stack traces are logged but never serialized to clients.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE = "https://api.services.com/problems/";

    // ---- 400 family ----------------------------------------------------------

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("field", fe.getField());
                    m.put("rejectedValue", fe.getRejectedValue());
                    m.put("message", fe.getDefaultMessage());
                    return m;
                })
                .toList();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation-failed",
                "Validation Failed",
                "One or more fields failed validation", req);
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .map(cv -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("path", cv.getPropertyPath().toString());
                    m.put("rejectedValue", cv.getInvalidValue());
                    m.put("message", cv.getMessage());
                    return m;
                })
                .toList();
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "constraint-violation",
                "Constraint Violation",
                "One or more constraints were violated", req);
        pd.setProperty("violations", violations);
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "missing-parameter", "Missing Request Parameter",
                "Required parameter '%s' is missing".formatted(ex.getParameterName()), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "type-mismatch", "Parameter Type Mismatch",
                "Parameter '%s' could not be converted to %s".formatted(
                        ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type"), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "malformed-body", "Malformed Request Body",
                "Request body is missing or unparseable", req);
    }

    // ---- 401 / 403 -----------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        // Authentication succeeded but the principal lacks the necessary authority.
        return problem(HttpStatus.FORBIDDEN, "access-denied", "Access Denied",
                "You do not have permission to perform this action", req);
    }

    // ---- 404 ----------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.NOT_FOUND, "resource-not-found", "Resource Not Found",
                ex.getMessage(), req);
        pd.setProperty("resourceType", ex.getResourceType());
        pd.setProperty("identifier", ex.getIdentifier());
        return pd;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "endpoint-not-found", "Endpoint Not Found",
                "No handler for %s %s".formatted(ex.getHttpMethod(), ex.getRequestURL()), req);
    }

    // ---- 405 ----------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", "Method Not Allowed",
                "HTTP method '%s' not allowed for this endpoint".formatted(ex.getMethod()), req);
        if (ex.getSupportedMethods() != null) {
            pd.setProperty("allowedMethods", ex.getSupportedMethods());
        }
        return pd;
    }

    // ---- 409 ----------------------------------------------------------------

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // Don't leak the SQL detail message — it can disclose schema info.
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "data-integrity", "Data Integrity Violation",
                "The request would violate a data integrity constraint", req);
    }

    // ---- 500 catch-all ------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        // Log full stack at WARN. Don't echo internals to the client.
        log.error("Unhandled exception at {} {}", req.getMethod(), req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                "An unexpected error occurred. Please retry, or contact support if the problem persists.", req);
    }

    // ---- builder ------------------------------------------------------------

    private static ProblemDetail problem(HttpStatus status, String typeSlug, String title,
                                         String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(PROBLEM_BASE + typeSlug));
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        // Trace IDs (set by Spring's actuator/observability) are convenient for support tickets.
        String traceId = req.getHeader("X-B3-TraceId");
        if (traceId == null) traceId = req.getHeader("traceparent");
        if (traceId != null) pd.setProperty("traceId", traceId);
        return pd;
    }
}
