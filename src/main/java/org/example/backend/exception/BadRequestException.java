package org.example.backend.exception;

/** Datos de entrada inválidos → 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
