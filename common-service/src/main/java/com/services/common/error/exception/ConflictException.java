package com.services.common.error.exception;

/**
 * Thrown when the request conflicts with current state (e.g. duplicate key, optimistic-lock failure).
 * Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
