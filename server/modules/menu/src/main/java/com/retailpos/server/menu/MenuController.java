package com.retailpos.server.menu;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    private final MenuService menus;

    public MenuController(MenuService menus) {
        this.menus = menus;
    }

    /** Whole menu configuration; the UI builds the folder tree and grid pages. */
    @GetMapping("/api/menus")
    public MenuService.MenuConfig menus() {
        return menus.config();
    }
}
