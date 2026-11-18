package com.retailpos.server.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {
    List<TaxRate> findAllByOrderByNameAsc();
}
