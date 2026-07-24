package com.example.scaffold.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    List<Category> findByParentIsNullOrderBySortOrderAsc();

    List<Category> findByParentIdOrderBySortOrderAsc(Long parentId);

    @Query("SELECT c.parent.id FROM Category c WHERE c.id = :id")
    Optional<Long> findParentId(Long id);
}
