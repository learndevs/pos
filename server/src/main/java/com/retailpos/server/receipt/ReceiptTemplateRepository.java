package com.retailpos.server.receipt;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptTemplateRepository extends JpaRepository<ReceiptTemplate, UUID> {
    Optional<ReceiptTemplate> findFirstByActiveTrue();
}
