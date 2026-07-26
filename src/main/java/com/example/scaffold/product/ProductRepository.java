package com.example.scaffold.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoriesId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE "
           + "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
           + "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR "
           + "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> search(String query, Pageable pageable);
}
