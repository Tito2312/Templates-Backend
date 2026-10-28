package org.example.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/** Clave compuesta de {@link TemplateUsage}: un registro por usuario y plantilla. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUsageId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;
}
