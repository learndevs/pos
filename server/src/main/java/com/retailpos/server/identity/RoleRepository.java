package com.retailpos.server.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    @Query(value = "SELECT permission FROM role_permission WHERE role_id = :roleId ORDER BY permission", nativeQuery = true)
    List<String> findPermissions(UUID roleId);
}
