package com.retailpos.server.admin;

import com.retailpos.server.catalog.Category;
import com.retailpos.server.catalog.CategoryRepository;
import com.retailpos.server.catalog.ItemClass;
import com.retailpos.server.catalog.Product;
import com.retailpos.server.catalog.ProductRepository;
import com.retailpos.server.catalog.TaxRate;
import com.retailpos.server.catalog.TaxRateRepository;
import com.retailpos.server.common.PosException;
import com.retailpos.server.sync.contract.CatalogMapper;
import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.Contract.CategoryV1;
import com.retailpos.server.sync.contract.Contract.ProductV1;
import com.retailpos.server.sync.contract.Contract.TaxRateV1;
import com.retailpos.server.sync.server.ChangeLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalog editing (tax rates, categories, products). Every save goes through the shared entity rules and
 * records the full record in the change log as a pending change (visible to tills after Publish).
 * Responses use the sync contract records, so the admin UI sees exactly what tills will receive.
 */
public final class AdminCatalog {

    private AdminCatalog() {}

    public record TaxRateIn(@NotBlank @Size(max = 60) String name, @Min(0) @Max(10000) int rateBp, boolean inclusive, boolean active) {}
    public record CategoryIn(@NotBlank @Size(max = 60) String name, int sortOrder, boolean active) {}
    public record ProductIn(@NotNull UUID categoryId, UUID taxRateId, @NotBlank @Size(max = 120) String name,
                            @NotNull ItemClass itemClass, String nonMerchType, @Size(max = 60) String sku,
                            @Size(max = 60) String barcode, @Min(0) long priceMinor, boolean kitchenPrint,
                            boolean trackStock, int sortOrder, boolean active) {}

    @Service
    @Transactional
    static class CatalogEditService {
        private final TaxRateRepository taxRates;
        private final CategoryRepository categories;
        private final ProductRepository products;
        private final ChangeLog changeLog;
        private final Clock clock;

        CatalogEditService(TaxRateRepository taxRates, CategoryRepository categories, ProductRepository products, ChangeLog changeLog, Clock clock) {
            this.taxRates = taxRates; this.categories = categories; this.products = products; this.changeLog = changeLog; this.clock = clock;
        }

        TaxRateV1 saveTaxRate(UUID id, TaxRateIn in, UUID by) {
            TaxRate t = id == null ? taxRates.save(TaxRate.create(null, in.name(), in.rateBp(), in.inclusive(), in.active())) : taxRates.findById(id).orElseThrow(() -> PosException.notFound("Tax rate not found."));
            if (id != null) t.update(in.name(), in.rateBp(), in.inclusive(), in.active());
            TaxRateV1 v = CatalogMapper.toV1(t);
            changeLog.record(Contract.Entity.TAX_RATE, t.getId(), v, by);
            return v;
        }

        CategoryV1 saveCategory(UUID id, CategoryIn in, UUID by) {
            Category c = id == null ? categories.save(Category.create(null, in.name(), in.sortOrder(), in.active())) : categories.findById(id).orElseThrow(() -> PosException.notFound("Category not found."));
            if (id != null) c.update(in.name(), in.sortOrder(), in.active());
            CategoryV1 v = CatalogMapper.toV1(c);
            changeLog.record(Contract.Entity.CATEGORY, c.getId(), v, by);
            return v;
        }

        ProductV1 saveProduct(UUID id, ProductIn in, UUID by) {
            Category cat = categories.findById(in.categoryId()).orElseThrow(() -> PosException.badRequest("Choose a category."));
            TaxRate tax = in.taxRateId() == null ? null : taxRates.findById(in.taxRateId()).orElseThrow(() -> PosException.badRequest("Unknown tax rate."));
            var data = new Product.Data(cat, tax, in.name(), in.itemClass(), in.nonMerchType(), in.sku(), in.barcode(),
                in.priceMinor(), in.kitchenPrint(), in.trackStock(), in.sortOrder(), in.active());
            Product p;
            if (id == null) {
                p = products.save(Product.create(null, data, clock.instant()));
            } else {
                p = products.findById(id).orElseThrow(() -> PosException.notFound("Product not found."));
                p.update(data, clock.instant());
            }
            ProductV1 v = CatalogMapper.toV1(p);
            changeLog.record(Contract.Entity.PRODUCT, p.getId(), v, by);
            return v;
        }

        @Transactional(readOnly = true)
        List<TaxRateV1> taxRates() { return taxRates.findAllByOrderByNameAsc().stream().map(CatalogMapper::toV1).toList(); }

        @Transactional(readOnly = true)
        List<CategoryV1> categories() { return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(CatalogMapper::toV1).toList(); }

        @Transactional(readOnly = true)
        List<ProductV1> products() {
            return products.findAll().stream().sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CatalogMapper::toV1).toList();
        }
    }

    @RestController
    @RequestMapping("/admin/api")
    static class CatalogController {
        private final CatalogEditService catalog;
        CatalogController(CatalogEditService catalog) { this.catalog = catalog; }

        @GetMapping("/tax-rates") List<TaxRateV1> taxRates() { return catalog.taxRates(); }
        @PostMapping("/tax-rates") TaxRateV1 createTax(@Valid @RequestBody TaxRateIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveTaxRate(null, in, me.id()); }
        @PutMapping("/tax-rates/{id}") TaxRateV1 updateTax(@PathVariable UUID id, @Valid @RequestBody TaxRateIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveTaxRate(id, in, me.id()); }

        @GetMapping("/categories") List<CategoryV1> categories() { return catalog.categories(); }
        @PostMapping("/categories") CategoryV1 createCategory(@Valid @RequestBody CategoryIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveCategory(null, in, me.id()); }
        @PutMapping("/categories/{id}") CategoryV1 updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveCategory(id, in, me.id()); }

        @GetMapping("/products") List<ProductV1> products() { return catalog.products(); }
        @PostMapping("/products") ProductV1 createProduct(@Valid @RequestBody ProductIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveProduct(null, in, me.id()); }
        @PutMapping("/products/{id}") ProductV1 updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductIn in, @AuthenticationPrincipal AdminAuth.Principal me) { return catalog.saveProduct(id, in, me.id()); }
    }
}
