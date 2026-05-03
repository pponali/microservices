package com.services.common.error.exception;

/**
 * Thrown when a requested resource cannot be located.
 * Maps to HTTP 404 via {@link com.services.common.error.GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object identifier;

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super("%s with id '%s' was not found".formatted(resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
