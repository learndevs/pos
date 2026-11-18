package com.retailpos.server.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MenuRepository extends JpaRepository<Menu, UUID> {}

interface MenuFolderRepository extends JpaRepository<MenuFolder, UUID> {}

interface MenuButtonRepository extends JpaRepository<MenuButton, UUID> {
    List<MenuButton> findByActiveTrueOrderByFolderIdAscPageAscGridRowAscGridColAsc();
}

interface PosFunctionRepository extends JpaRepository<PosFunction, String> {}
