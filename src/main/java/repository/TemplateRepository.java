package repository;

import model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT t FROM Template t " +
            "WHERE t.user.id = :userId " +
            "AND (:brandId IS NULL OR t.brand.id = :brandId) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:query IS NULL OR :query = '' OR " +
            "     LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "     LOWER(t.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY t.usageCount DESC, t.updatedAt DESC")
    List<Template> searchTemplates(@Param("userId") Long userId,
                                   @Param("query") String query,
                                   @Param("brandId") Long brandId,
                                   @Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Template t SET t.usageCount = t.usageCount + 1 WHERE t.id = :id AND t.user.id = :userId")
    int incrementUsageCount(@Param("id") Long id, @Param("userId") Long userId);

}
