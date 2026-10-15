package org.example.backend.repository;

import org.example.backend.model.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT t FROM Template t " +
            "LEFT JOIN t.brand b " +
            "LEFT JOIN t.category c " +
            "WHERE t.user.id = :userId " +
            "AND (:query IS NULL OR :query = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:brandName IS NULL OR :brandName = '' OR b.name = :brandName OR CAST(b.id AS string) = :brandName) " +
            "AND (:categoryName IS NULL OR :categoryName = '' OR c.name = :categoryName OR CAST(c.id AS string) = :categoryName)")
    Page<Template> searchTemplates(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("brandName") String brandName,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );

    List<Template> findByUserId(Long userId);
}