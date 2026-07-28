package com.example.scaffold.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariationRepository extends JpaRepository<ProductVariation, Long> {

    @EntityGraph(attributePaths = "product")
    Page<ProductVariation> findByInventoryCountLessThanEqualOrderByInventoryCountAsc(int threshold, Pageable pageable);

    long countByInventoryCountLessThanEqual(int threshold);
}
