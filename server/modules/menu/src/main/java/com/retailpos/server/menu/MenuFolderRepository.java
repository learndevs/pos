package com.retailpos.server.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuFolderRepository extends JpaRepository<MenuFolder, UUID> {
    List<MenuFolder> findByActiveTrue();
    List<MenuFolder> findByParentFolderIdAndActiveTrue(UUID parentFolderId);
}
