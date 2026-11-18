package com.retailpos.server.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndActiveTrue(UUID id);

    @Query("""
        SELECT p FROM Product p JOIN FETCH p.category
        WHERE p.active AND (lower(p.name) LIKE lower(concat('%', :term, '%')) OR p.sku = :term OR p.barcode = :term)
        ORDER BY p.name""")
    List<Product> search(String term, Pageable page);
}
