package com.retailpos.server.sync.contract;

import com.retailpos.server.catalog.Category;
import com.retailpos.server.catalog.Product;
import com.retailpos.server.catalog.TaxRate;
import com.retailpos.server.sync.contract.Contract.CategoryV1;
import com.retailpos.server.sync.contract.Contract.ProductV1;
import com.retailpos.server.sync.contract.Contract.TaxRateV1;

/** Entity -> contract record. The till goes back through the entities' own create/update rules. */
public final class CatalogMapper {

    private CatalogMapper() {}

    public static TaxRateV1 toV1(TaxRate t) {
        return new TaxRateV1(t.getId(), t.getName(), t.getRateBp(), t.isInclusive(), t.isActive());
    }

    public static CategoryV1 toV1(Category c) {
        return new CategoryV1(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
    }

    public static ProductV1 toV1(Product p) {
        return new ProductV1(p.getId(), p.getCategory().getId(), p.getTaxRate() == null ? null : p.getTaxRate().getId(),
            p.getName(), p.getItemClass().name(), p.getNonMerchType(), p.getSku(), p.getBarcode(), p.getPriceMinor(),
            p.isKitchenPrint(), p.isTrackStock(), p.getSortOrder(), p.isActive());
    }
}
