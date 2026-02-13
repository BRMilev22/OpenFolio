package com.openfolio.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Long id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "%s with id %d not found".formatted(resource, id));
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "%s '%s' not found".formatted(resource, identifier));
    }
}
