package org.example.backend.exception;

/** La operación choca con el estado actual (duplicado, recurso en uso...) → 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
