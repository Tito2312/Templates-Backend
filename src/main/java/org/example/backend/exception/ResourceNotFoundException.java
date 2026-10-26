package org.example.backend.exception;

/** El recurso solicitado no existe → 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
