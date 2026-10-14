package org.example.backend.repository;

import org.example.backend.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByUserIdOrderByNameAsc(Long userId);

    List<Brand> findByUserId(Long id);
}
