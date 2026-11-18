package com.retailpos.server.sync.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Records exchanged between admin and POS. Shared jar: both sides use exactly these types. */
public final class Contract {

    public static final int CONTRACT_VERSION = 1;

    /** Entity kinds in apply order (references first). */
    public enum Entity { TAX_RATE, CATEGORY, PRODUCT, MENU_FOLDER, MENU_BUTTON }

    private Contract() {}

    // ---------- master data (admin -> POS) ----------

    public record TaxRateV1(UUID id, String name, int rateBp, boolean inclusive, boolean active) {}

    public record CategoryV1(UUID id, String name, int sortOrder, boolean active) {}

    public record ProductV1(UUID id, UUID categoryId, UUID taxRateId, String name, String itemClass, String nonMerchType,
                            String sku, String barcode, long priceMinor, boolean kitchenPrint, boolean trackStock,
                            int sortOrder, boolean active) {}

    /** A sub-folder of a menu (SALE / TENDER / ITEM); root folders exist on every till already. */
    public record MenuFolderV1(UUID id, UUID menuId, UUID parentFolderId, String name, boolean active) {}

    /** One slot of a 4 x 3 page; inactive = removed (frees the slot). */
    public record MenuButtonV1(UUID id, UUID folderId, int page, int gridRow, int gridCol, String label, String color,
                               String actionType, UUID productId, UUID targetFolderId, String tenderMethod, Long amountMinor,
                               String functionCode, UUID targetMenuId, boolean active) {}

    /** manifest.json inside a package zip. sha256: file name -> SHA-256 hex of that file. */
    public record Manifest(int contractVersion, int packageNo, Instant createdAt, String scope, LocalDate dateFrom,
                           LocalDate dateTo, List<Entity> entities, Map<String, Integer> counts,
                           Map<String, String> sha256, String note) {}

    /** The data of a package (one list per entity file). */
    public record PackageContent(List<TaxRateV1> taxRates, List<CategoryV1> categories, List<ProductV1> products,
                                 List<MenuFolderV1> menuFolders, List<MenuButtonV1> menuButtons) {
        public PackageContent(List<TaxRateV1> taxRates, List<CategoryV1> categories, List<ProductV1> products) {
            this(taxRates, categories, products, List.of(), List.of());
        }
        public int size() { return taxRates.size() + categories.size() + products.size() + menuFolders.size() + menuButtons.size(); }
    }

    /** GET /sync/v1/packages: what is waiting for this till, oldest first. */
    public record PackageInfo(int packageNo, Instant createdAt, String scope, LocalDate dateFrom, LocalDate dateTo,
                              List<Entity> entities, Map<String, Integer> counts, String sha256, int sizeBytes, String note) {}

    /** POST /sync/v1/packages/{no}/ack */
    public record PackageAck(String status, String error) {
        public static PackageAck applied() { return new PackageAck("APPLIED", null); }
        public static PackageAck failed(String error) { return new PackageAck("FAILED", error); }
    }

    // ---------- transactions (POS -> admin archive) ----------

    public record TransactionLineV1(int lineNo, String kind, String name, UUID productId, String itemClass,
                                    BigDecimal quantity, Long unitPriceMinor, Long discountMinor, Integer discountPercent,
                                    Long taxMinor, long amountMinor, String tenderMethod, Long tenderedMinor, Long changeMinor) {}

    /** GET /sync/v1/me: who this till is (assigned in admin). */
    public record TerminalInfo(UUID terminalId, String code, String name, String location, Integer tillNo) {}

    public record TransactionV1(UUID id, String deviceCode, long txnNo, String receiptNo, String type, String status, String orderType,
                                LocalDate businessDate, Instant createdAt, Instant completedAt, Instant cancelledAt,
                                String reason, UUID cashierId, String cashierName, long subtotalMinor, long taxMinor,
                                long totalMinor, long paidMinor, long changeMinor, List<TransactionLineV1> lines) {}

    /** POST /sync/v1/transactions body / response. */
    public record TransactionBatch(int contractVersion, List<TransactionV1> transactions) {}

    public record BatchResult(int received, int stored) {}
}
