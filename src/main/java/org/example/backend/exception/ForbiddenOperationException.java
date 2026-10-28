package org.example.backend.exception;

/** El usuario autenticado no tiene permiso sobre este recurso → 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
