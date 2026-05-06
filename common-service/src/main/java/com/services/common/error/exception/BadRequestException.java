package com.services.common.error.exception;

/**
 * Thrown when the caller sent a syntactically valid but semantically invalid request.
 * Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
