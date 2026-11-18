package com.retailpos.server.menu;

import com.retailpos.server.catalog.Product;
import com.retailpos.server.catalog.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

    public record MenuDto(UUID id, String code, String name, UUID rootFolderId) {}

    public record FolderDto(UUID id, UUID menuId, UUID parentFolderId, String name) {}

    public record ButtonDto(UUID id, UUID folderId, int page, int gridRow, int gridCol, String label, String color,
                            String actionType, UUID productId, Long productPriceMinor, UUID targetFolderId,
                            String tenderMethod, Long amountMinor, String functionCode, String functionInput,
                            Boolean needsSelectedLine, Boolean functionAvailable, String requiredPermission,
                            String targetMenuCode) {}

    public record MenuConfig(List<MenuDto> menus, List<FolderDto> folders, List<ButtonDto> buttons) {}

    private final MenuRepository menus;
    private final MenuFolderRepository folders;
    private final MenuButtonRepository buttons;
    private final PosFunctionRepository functions;
    private final ProductRepository products;

    MenuService(MenuRepository menus, MenuFolderRepository folders, MenuButtonRepository buttons,
                PosFunctionRepository functions, ProductRepository products) {
        this.menus = menus;
        this.folders = folders;
        this.buttons = buttons;
        this.functions = functions;
        this.products = products;
    }

    /** The whole configuration in one go (small): menus, folder tree, active buttons with resolved details. */
    public MenuConfig config() {
        List<MenuFolder> allFolders = folders.findAll();
        Map<UUID, Menu> menuById = menus.findAll().stream().collect(Collectors.toMap(Menu::getId, Function.identity()));
        Map<String, PosFunction> fnByCode = functions.findAll().stream().collect(Collectors.toMap(PosFunction::getCode, Function.identity()));
        List<MenuButton> active = buttons.findByActiveTrueOrderByFolderIdAscPageAscGridRowAscGridColAsc();
        Map<UUID, Product> productById = products.findAllById(
                active.stream().map(MenuButton::getProductId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        List<MenuDto> menuDtos = allFolders.stream().filter(MenuFolder::isRoot)
            .map(f -> {
                Menu m = menuById.get(f.getMenuId());
                return new MenuDto(m.getId(), m.getCode(), m.getName(), f.getId());
            }).toList();

        List<FolderDto> folderDtos = allFolders.stream()
            .map(f -> new FolderDto(f.getId(), f.getMenuId(), f.getParentFolderId(), f.getName())).toList();

        List<ButtonDto> buttonDtos = active.stream()
            .filter(b -> b.getProductId() == null || (productById.containsKey(b.getProductId()) && productById.get(b.getProductId()).isActive()))
            .map(b -> {
                Product p = b.getProductId() == null ? null : productById.get(b.getProductId());
                PosFunction fn = b.getFunctionCode() == null ? null : fnByCode.get(b.getFunctionCode());
                Menu target = b.getTargetMenuId() == null ? null : menuById.get(b.getTargetMenuId());
                return new ButtonDto(b.getId(), b.getFolderId(), b.getPage(), b.getGridRow(), b.getGridCol(), b.getLabel(),
                    b.getColor(), b.getActionType().name(), b.getProductId(), p == null ? null : p.getPriceMinor(),
                    b.getTargetFolderId(), b.getTenderMethod(), b.getAmountMinor(), b.getFunctionCode(),
                    fn == null ? null : fn.getInput(), fn == null ? null : fn.isNeedsSelectedLine(),
                    fn == null ? null : fn.isAvailable(), fn == null ? null : fn.getRequiredPermission(),
                    target == null ? null : target.getCode());
            }).toList();

        return new MenuConfig(menuDtos, folderDtos, buttonDtos);
    }
}
