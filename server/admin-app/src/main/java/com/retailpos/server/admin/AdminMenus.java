package com.retailpos.server.admin;

import com.retailpos.server.common.PosException;
import com.retailpos.server.menu.ActionType;
import com.retailpos.server.menu.MenuButton;
import com.retailpos.server.menu.MenuButtonRepository;
import com.retailpos.server.menu.MenuFolder;
import com.retailpos.server.menu.MenuFolderRepository;
import com.retailpos.server.menu.MenuRepository;
import com.retailpos.server.menu.MenuService;
import com.retailpos.server.menu.PosFunction;
import com.retailpos.server.menu.PosFunctionRepository;
import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.MenuMapper;
import com.retailpos.server.sync.server.ChangeLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Menu configuration (folders + 4 x 3 button grid) for the SALE / TENDER / ITEM menus. Uses the shared menu
 * entities and rules; every change goes to the change log so it can be broadcast to tills.
 */
public final class AdminMenus {

    private AdminMenus() {}

    public record FolderIn(UUID menuId, UUID parentFolderId, @NotBlank @Size(max = 40) String name) {}
    public record ButtonIn(@NotNull UUID folderId, int page, int gridRow, int gridCol, @NotBlank @Size(max = 40) String label,
                           String color, @NotNull ActionType actionType, UUID productId, UUID targetFolderId,
                           String tenderMethod, Long amountMinor, String functionCode, UUID targetMenuId) {}
    public record FunctionView(String code, String name, String input, String requiredPermission, boolean needsSelectedLine, boolean available) {}

    @Service
    @Transactional
    static class MenuEditService {
        private final MenuRepository menus;
        private final MenuFolderRepository folders;
        private final MenuButtonRepository buttons;
        private final ChangeLog changeLog;

        MenuEditService(MenuRepository menus, MenuFolderRepository folders, MenuButtonRepository buttons, ChangeLog changeLog) {
            this.menus = menus; this.folders = folders; this.buttons = buttons; this.changeLog = changeLog;
        }

        Contract.MenuFolderV1 createFolder(FolderIn in, UUID by) {
            MenuFolder parent = activeFolder(in.parentFolderId());
            MenuFolder f = folders.save(MenuFolder.create(null, parent.getMenuId(), parent.getId(), in.name(), true));
            return log(f, by);
        }

        Contract.MenuFolderV1 renameFolder(UUID id, String name, UUID by) {
            MenuFolder f = activeFolder(id);
            f.update(name, true);
            return log(f, by);
        }

        /** Removes a folder with everything inside it, and the buttons that open it. */
        void removeFolder(UUID id, UUID by) {
            MenuFolder f = activeFolder(id);
            if (f.isRoot()) throw PosException.badRequest("A menu's root folder cannot be removed.");
            for (MenuFolder child : folders.findByParentFolderIdAndActiveTrue(id)) removeFolder(child.getId(), by);
            for (MenuButton b : buttons.findByFolderIdAndActiveTrue(id)) removeButton(b, by);
            for (MenuButton b : buttons.findByTargetFolderIdAndActiveTrue(id)) removeButton(b, by);
            f.update(f.getName(), false);
            log(f, by);
        }

        Contract.MenuButtonV1 saveButton(UUID id, ButtonIn in, UUID by) {
            MenuFolder folder = activeFolder(in.folderId());
            if (in.actionType() == ActionType.OPEN_FOLDER) {
                MenuFolder target = activeFolder(in.targetFolderId());
                if (!target.getMenuId().equals(folder.getMenuId())) throw PosException.badRequest("The folder must be in the same menu.");
                if (target.getId().equals(folder.getId())) throw PosException.badRequest("A folder button cannot open its own folder.");
            }
            if (in.actionType() == ActionType.SWITCH_MENU && (in.targetMenuId() == null || !menus.existsById(in.targetMenuId()))) {
                throw PosException.badRequest("Choose the menu to switch to.");
            }
            UUID self = id == null ? new UUID(0, 0) : id;
            if (buttons.existsByFolderIdAndPageAndGridRowAndGridColAndActiveTrueAndIdNot(in.folderId(), in.page(), in.gridRow(), in.gridCol(), self)) {
                throw PosException.conflict("That slot already has a button. Remove or move it first.");
            }
            var data = new MenuButton.Data(in.folderId(), in.page(), in.gridRow(), in.gridCol(), in.label(), in.color(), in.actionType(),
                in.productId(), in.targetFolderId(), in.tenderMethod(), in.amountMinor(), in.functionCode(), in.targetMenuId(), true);
            MenuButton b;
            if (id == null) {
                b = buttons.save(MenuButton.create(null, data));
            } else {
                b = buttons.findById(id).filter(MenuButton::isActive).orElseThrow(() -> PosException.notFound("Button not found."));
                b.update(data);
            }
            return log(b, by);
        }

        void removeButton(UUID id, UUID by) {
            removeButton(buttons.findById(id).filter(MenuButton::isActive).orElseThrow(() -> PosException.notFound("Button not found.")), by);
        }

        private void removeButton(MenuButton b, UUID by) {
            b.remove();
            log(b, by);
        }

        private MenuFolder activeFolder(UUID id) {
            if (id == null) throw PosException.badRequest("Choose a folder.");
            return folders.findById(id).filter(MenuFolder::isActive).orElseThrow(() -> PosException.notFound("Folder not found."));
        }

        private Contract.MenuFolderV1 log(MenuFolder f, UUID by) {
            folders.flush();
            var v = MenuMapper.toV1(f);
            changeLog.record(Contract.Entity.MENU_FOLDER, f.getId(), v, by);
            return v;
        }

        private Contract.MenuButtonV1 log(MenuButton b, UUID by) {
            buttons.flush();
            var v = MenuMapper.toV1(b);
            changeLog.record(Contract.Entity.MENU_BUTTON, b.getId(), v, by);
            return v;
        }
    }

    @RestController
    @RequestMapping("/admin/api")
    static class MenuController {
        record Rename(@NotBlank @Size(max = 40) String name) {}

        private final MenuService menuService;
        private final MenuEditService edit;
        private final PosFunctionRepository functions;

        MenuController(MenuService menuService, MenuEditService edit, PosFunctionRepository functions) {
            this.menuService = menuService; this.edit = edit; this.functions = functions;
        }

        /** Same shape the till uses (menus, folders, active buttons). */
        @GetMapping("/menus") MenuService.MenuConfig menus() { return menuService.config(); }

        @GetMapping("/pos-functions")
        List<FunctionView> functions() {
            return functions.findAll().stream().sorted(java.util.Comparator.comparing(PosFunction::getName))
                .map(f -> new FunctionView(f.getCode(), f.getName(), f.getInput(), f.getRequiredPermission(), f.isNeedsSelectedLine(), f.isAvailable())).toList();
        }

        @PostMapping("/menu-folders") Contract.MenuFolderV1 createFolder(@Valid @RequestBody FolderIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return edit.createFolder(in, me.id()); }
        @PutMapping("/menu-folders/{id}") Contract.MenuFolderV1 renameFolder(@PathVariable UUID id, @Valid @RequestBody Rename in, @AuthenticationPrincipal AdminAuth.Principal me) { return edit.renameFolder(id, in.name(), me.id()); }
        @DeleteMapping("/menu-folders/{id}") void removeFolder(@PathVariable UUID id, @AuthenticationPrincipal AdminAuth.Principal me) { edit.removeFolder(id, me.id()); }

        @PostMapping("/menu-buttons") Contract.MenuButtonV1 createButton(@Valid @RequestBody ButtonIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return edit.saveButton(null, in, me.id()); }
        @PutMapping("/menu-buttons/{id}") Contract.MenuButtonV1 updateButton(@PathVariable UUID id, @Valid @RequestBody ButtonIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return edit.saveButton(id, in, me.id()); }
        @DeleteMapping("/menu-buttons/{id}") void removeButton(@PathVariable UUID id, @AuthenticationPrincipal AdminAuth.Principal me) { edit.removeButton(id, me.id()); }
    }
}
