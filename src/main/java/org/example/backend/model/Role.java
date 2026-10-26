package org.example.backend.model;

/**
 * Roles de la aplicación. El nombre se guarda tal cual en users.role
 * y se usa directamente como authority de Spring Security.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
