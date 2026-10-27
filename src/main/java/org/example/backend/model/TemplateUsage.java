package org.example.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Veces que un usuario ha copiado una plantilla (el uso es individual, no global).
 * Tabla creada por la migración V1_0_4_003.
 */
@Entity
@Table(name = "template_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateUsage {

    @EmbeddedId
    private TemplateUsageId id;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;
}
