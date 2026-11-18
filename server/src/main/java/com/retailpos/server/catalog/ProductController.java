package com.retailpos.server.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    public record ProductHit(UUID id, String name, long priceMinor, String category, String sku, String barcode) {}

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    /** Product search for the sale screen: name contains, or exact SKU / barcode. Max 30 results. */
    @GetMapping("/api/products/search")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @Transactional(readOnly = true)
    public List<ProductHit> search(@RequestParam(defaultValue = "") String q) {
        String term = q.trim();
        if (term.isEmpty()) return List.of();
        return products.search(term, PageRequest.of(0, 30)).stream()
            .map(p -> new ProductHit(p.getId(), p.getName(), p.getPriceMinor(), p.getCategory().getName(), p.getSku(), p.getBarcode()))
            .toList();
    }
}
