package org.example.backend.repository;

import org.example.backend.model.TemplateUsage;
import org.example.backend.model.TemplateUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TemplateUsageRepository extends JpaRepository<TemplateUsage, TemplateUsageId> {

    // Suma 1 al uso existente en una sola sentencia (sin leer antes). Devuelve 0 si aún no hay registro.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TemplateUsage u SET u.usageCount = u.usageCount + 1, u.lastUsedAt = :now " +
            "WHERE u.id.userId = :userId AND u.id.templateId = :templateId")
    int increment(@Param("userId") Long userId, @Param("templateId") Long templateId, @Param("now") LocalDateTime now);

    // Usos de un usuario para las plantillas de la página actual
    @Query("SELECT u FROM TemplateUsage u WHERE u.id.userId = :userId AND u.id.templateId IN :templateIds")
    List<TemplateUsage> findByUserAndTemplates(@Param("userId") Long userId, @Param("templateIds") Collection<Long> templateIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM TemplateUsage u WHERE u.id.templateId = :templateId")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
