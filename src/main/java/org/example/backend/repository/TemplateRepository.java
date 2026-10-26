package org.example.backend.repository;

import org.example.backend.model.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    // Catálogo global: busca en las plantillas de todos los usuarios.
    // Marca y categoría se filtran por nombre (sin distinguir mayúsculas) o por id.
    @Query("SELECT t FROM Template t " +
            "LEFT JOIN t.brand b " +
            "LEFT JOIN t.category c " +
            "WHERE (:query IS NULL OR :query = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:brandName IS NULL OR :brandName = '' OR LOWER(b.name) = LOWER(:brandName) OR CAST(b.id AS string) = :brandName) " +
            "AND (:categoryName IS NULL OR :categoryName = '' OR LOWER(c.name) = LOWER(:categoryName) OR CAST(c.id AS string) = :categoryName)")
    Page<Template> searchTemplates(
            @Param("query") String query,
            @Param("brandName") String brandName,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );

    // Al eliminar una marca/categoría sus plantillas se conservan y quedan sin ella ("globales")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Template t SET t.brand = null WHERE t.brand.id = :brandId")
    int unlinkBrand(@Param("brandId") Long brandId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Template t SET t.category = null WHERE t.category.id = :categoryId")
    int unlinkCategory(@Param("categoryId") Long categoryId);
}
