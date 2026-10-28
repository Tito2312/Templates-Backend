package org.example.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Formato único de error de la API (lo usan el manejador global
 * y los handlers de 401/403 de Spring Security).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
