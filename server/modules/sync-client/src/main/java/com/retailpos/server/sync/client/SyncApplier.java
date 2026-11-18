package com.retailpos.server.sync.client;

import com.retailpos.server.catalog.Category;
import com.retailpos.server.catalog.CategoryRepository;
import com.retailpos.server.catalog.ItemClass;
import com.retailpos.server.catalog.Product;
import com.retailpos.server.catalog.ProductRepository;
import com.retailpos.server.catalog.TaxRate;
import com.retailpos.server.catalog.TaxRateRepository;
import com.retailpos.server.common.PosException;
import com.retailpos.server.menu.MenuButton;
import com.retailpos.server.menu.MenuButtonRepository;
import com.retailpos.server.menu.MenuFolder;
import com.retailpos.server.menu.MenuFolderRepository;
import com.retailpos.server.sync.contract.Contract.MenuFolderV1;
import com.retailpos.server.sync.contract.MenuMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.retailpos.server.sync.contract.Contract.Manifest;
import com.retailpos.server.sync.contract.Contract.PackageContent;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a verified package in ONE transaction: all or nothing. Upserts by id through the same entity
 * create/update rules the admin app used; references first (tax, category, product).
 */
@Service
public class SyncApplier {

    private final TaxRateRepository taxRates;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final MenuFolderRepository folders;
    private final MenuButtonRepository buttons;
    private final SyncStateRepository state;
    private final Clock clock;

    SyncApplier(TaxRateRepository taxRates, CategoryRepository categories, ProductRepository products,
                MenuFolderRepository folders, MenuButtonRepository buttons, SyncStateRepository state, Clock clock) {
        this.taxRates = taxRates;
        this.categories = categories;
        this.products = products;
        this.folders = folders;
        this.buttons = buttons;
        this.state = state;
        this.clock = clock;
    }

    @Transactional
    public void apply(Manifest manifest, PackageContent c) {
        for (var v : c.taxRates()) {
            taxRates.findById(v.id()).ifPresentOrElse(
                t -> t.update(v.name(), v.rateBp(), v.inclusive(), v.active()),
                () -> taxRates.save(TaxRate.create(v.id(), v.name(), v.rateBp(), v.inclusive(), v.active())));
        }
        for (var v : c.categories()) {
            categories.findById(v.id()).ifPresentOrElse(
                cat -> cat.update(v.name(), v.sortOrder(), v.active()),
                () -> categories.save(Category.create(v.id(), v.name(), v.sortOrder(), v.active())));
        }
        taxRates.flush();
        categories.flush();
        for (var v : c.products()) {
            Category category = categories.findById(v.categoryId())
                .orElseThrow(() -> PosException.conflict("Package refers to an unknown category " + v.categoryId() + "."));
            TaxRate tax = v.taxRateId() == null ? null : taxRates.findById(v.taxRateId())
                .orElseThrow(() -> PosException.conflict("Package refers to an unknown tax rate " + v.taxRateId() + "."));
            var data = new Product.Data(category, tax, v.name(), ItemClass.valueOf(v.itemClass()), v.nonMerchType(),
                v.sku(), v.barcode(), v.priceMinor(), v.kitchenPrint(), v.trackStock(), v.sortOrder(), v.active());
            products.findById(v.id()).ifPresentOrElse(
                p -> p.update(data, clock.instant()),
                () -> products.save(Product.create(v.id(), data, clock.instant())));
        }
        products.flush();
        applyMenus(c);
        state.findById("admin").orElseThrow().packageApplied(manifest.packageNo(), clock.instant());
    }

    private void applyMenus(PackageContent c) {
        // folders: parents first (a parent may be in the same package)
        Map<UUID, MenuFolderV1> incoming = new HashMap<>();
        c.menuFolders().forEach(f -> incoming.put(f.id(), f));
        java.util.Set<UUID> done = new java.util.HashSet<>();
        for (MenuFolderV1 f : c.menuFolders()) applyFolder(f, incoming, done);
        folders.flush();

        // buttons, two phases: take every incoming button out of the grid first, so moves / swaps
        // inside one package never collide on the "one active button per slot" rule
        for (var v : c.menuButtons()) buttons.findById(v.id()).ifPresent(MenuButton::remove);
        buttons.flush();
        for (var v : c.menuButtons()) {
            var data = MenuMapper.data(v);
            buttons.findById(v.id()).ifPresentOrElse(b -> b.update(data), () -> buttons.save(MenuButton.create(v.id(), data)));
        }
        buttons.flush();
    }

    private void applyFolder(MenuFolderV1 f, Map<UUID, MenuFolderV1> incoming, java.util.Set<UUID> done) {
        if (!done.add(f.id())) return;
        MenuFolderV1 parent = incoming.get(f.parentFolderId());
        if (parent != null) applyFolder(parent, incoming, done);
        folders.findById(f.id()).ifPresentOrElse(
            existing -> existing.update(f.name(), f.active()),
            () -> folders.save(MenuFolder.create(f.id(), f.menuId(), f.parentFolderId(), f.name(), f.active())));
    }
}
