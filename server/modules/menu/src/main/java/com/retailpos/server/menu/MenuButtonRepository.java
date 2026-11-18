package com.retailpos.server.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuButtonRepository extends JpaRepository<MenuButton, UUID> {
    List<MenuButton> findByActiveTrueOrderByFolderIdAscPageAscGridRowAscGridColAsc();
    List<MenuButton> findByFolderIdAndActiveTrue(UUID folderId);
    List<MenuButton> findByTargetFolderIdAndActiveTrue(UUID folderId);
    boolean existsByFolderIdAndPageAndGridRowAndGridColAndActiveTrueAndIdNot(UUID folderId, int page, int gridRow, int gridCol, UUID id);
}
