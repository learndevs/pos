package com.retailpos.server.receipt;

import com.retailpos.server.common.PosException;
import com.retailpos.server.identity.AppUserRepository;
import com.retailpos.server.sales.PosTransaction;
import com.retailpos.server.sales.PosTransactionRepository;
import com.retailpos.server.sales.SaleItem;
import com.retailpos.server.sales.TenderItem;
import com.retailpos.server.settings.AppSetting;
import com.retailpos.server.settings.AppSettingRepository;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders the active receipt template with data from a completed transaction.
 * Values are HTML-escaped by Mustache; money is formatted with the business currency.
 */
@Service
@Transactional
public class ReceiptService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ReceiptTemplateRepository templates;
    private final PosTransactionRepository transactions;
    private final AppSettingRepository settings;
    private final AppUserRepository users;
    private final Clock clock;
    private final Mustache.Compiler compiler = Mustache.compiler().defaultValue("").emptyStringIsFalse(true).zeroIsFalse(false).escapeHTML(true);

    public ReceiptService(ReceiptTemplateRepository templates, PosTransactionRepository transactions,
                          AppSettingRepository settings, AppUserRepository users, Clock clock) {
        this.templates = templates;
        this.transactions = transactions;
        this.settings = settings;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReceiptTemplate activeTemplate() {
        return templates.findFirstByActiveTrue().orElseThrow(() -> PosException.notFound("No active receipt template."));
    }

    /** Receipt HTML for a completed sale, using the active template. */
    @Transactional(readOnly = true)
    public String render(UUID txnId) {
        PosTransaction t = transactions.findById(txnId).orElseThrow(() -> PosException.notFound("Sale not found."));
        if (!PosTransaction.COMPLETED.equals(t.getStatus())) throw PosException.conflict("Receipts are printed for completed sales only.");
        return render(activeTemplate().getHtml(), model(t));
    }

    /** Renders any template HTML (e.g. an unsaved edit) against a sale, or sample data if none given. */
    @Transactional(readOnly = true)
    public String preview(String html, UUID txnId) {
        Map<String, Object> model = txnId == null ? sampleModel()
            : model(transactions.findById(txnId).orElseThrow(() -> PosException.notFound("Sale not found.")));
        return render(html, model);
    }

    public ReceiptTemplate updateActive(String name, String html, int paperWidthMm) {
        render(html, sampleModel());                      // reject templates that do not compile
        ReceiptTemplate t = activeTemplate();
        t.update(name, html, paperWidthMm, clock.instant());
        return templates.save(t);
    }

    private String render(String html, Map<String, Object> model) {
        try {
            return compiler.compile(html).execute(model);
        } catch (MustacheException e) {
            throw PosException.badRequest("Receipt template error: " + e.getMessage());
        }
    }

    // ---------- model ----------

    private Map<String, Object> model(PosTransaction t) {
        Map<String, Object> business = json("business");
        Money money = money(business);
        ZoneId zone = clock.getZone().equals(ZoneId.of("Z")) ? ZoneId.systemDefault() : clock.getZone();
        var completed = (t.getCompletedAt() == null ? clock.instant() : t.getCompletedAt()).atZone(zone);

        List<Map<String, Object>> lines = t.saleItems().stream().map(i -> {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("qty", qty(i.getQuantity()));
            l.put("name", i.getItemName());
            l.put("unitPrice", money.format(i.getUnitPriceMinor()));
            l.put("amount", money.format(i.getAmountMinor() + i.getDiscountMinor()));   // before line discount
            l.put("net", money.format(i.getAmountMinor()));                            // after line discount
            l.put("showUnit", i.getQuantity().compareTo(BigDecimal.ONE) != 0);
            l.put("hasDiscount", i.getDiscountMinor() > 0);
            l.put("discount", money.format(i.getDiscountMinor()));
            l.put("discountPercent", i.getDiscountPercent());
            return l;
        }).toList();

        List<Map<String, Object>> tenders = t.tenderItems().stream().map(tn -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("method", label(tn.getMethod()));
            m.put("amount", money.format(tn.getAmountMinor()));
            m.put("tendered", money.format(tn.getTenderedMinor() != null ? tn.getTenderedMinor() : tn.getAmountMinor()));
            return m;
        }).toList();

        Map<String, Object> sale = new LinkedHashMap<>();
        sale.put("receiptNo", t.getReceiptNo());
        sale.put("txnNo", t.getTxnNo());
        sale.put("date", DATE.format(completed));
        sale.put("time", TIME.format(completed));
        sale.put("businessDate", t.getBusinessDate().toString());
        sale.put("orderType", "DINE_IN".equals(t.getOrderType()) ? "Dine in" : "Takeaway");
        sale.put("cashier", users.findById(t.getCreatedBy()).map(u -> u.getDisplayName()).orElse(""));

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subtotal", money.format(t.getSubtotalMinor()));
        totals.put("tax", money.format(t.getTaxMinor()));
        totals.put("total", money.format(t.getTotalMinor()));
        totals.put("paid", money.format(t.paidMinor()));
        totals.put("change", money.format(t.changeMinor()));
        totals.put("hasChange", t.changeMinor() > 0);

        Map<String, Object> device = new LinkedHashMap<>();
        device.put("code", t.getDeviceCode() == null ? "" : t.getDeviceCode());
        device.put("name", json("device").getOrDefault("name", ""));
        return Map.of("business", business, "receipt", json("receipt"), "sale", sale, "lines", lines,
            "tenders", tenders, "totals", totals, "device", device);
    }

    private Map<String, Object> sampleModel() {
        Map<String, Object> business = json("business");
        Money money = money(business);
        return Map.of(
            "business", business,
            "receipt", json("receipt"),
            "device", Map.of("code", "T01", "name", "Till 1"),
            "sale", Map.of("receiptNo", "20260101-0001", "txnNo", 1001, "date", "2026-01-01", "time", "12:30",
                "businessDate", "2026-01-01", "orderType", "Takeaway", "cashier", "Sample"),
            "lines", List.of(
                Map.of("qty", "1", "name", "Burger", "unitPrice", money.format(1000), "amount", money.format(1000), "showUnit", false),
                Map.of("qty", "2", "name", "Cola", "unitPrice", money.format(250), "amount", money.format(500), "showUnit", true)),
            "tenders", List.of(Map.of("method", "Cash", "amount", money.format(1500), "tendered", money.format(2000))),
            "totals", Map.of("subtotal", money.format(1500), "tax", money.format(136), "total", money.format(1500),
                "paid", money.format(1500), "change", money.format(500), "hasChange", true));
    }

    private Map<String, Object> json(String key) {
        return settings.findById(key).map(AppSetting::getValue)
            .map(v -> JsonParserFactory.getJsonParser().parseMap(v)).orElse(Map.of());
    }

    private static String qty(BigDecimal q) {
        return q.stripTrailingZeros().toPlainString();
    }

    private static String label(TenderItem.Method m) {
        return switch (m) {
            case CASH -> "Cash";
            case CARD -> "Card";
            case OTHER -> "Other";
        };
    }

    private record Money(NumberFormat format, int digits) {
        String format(long minor) {
            return format.format(BigDecimal.valueOf(minor, digits));
        }
    }

    private static Money money(Map<String, Object> business) {
        String code = String.valueOf(business.getOrDefault("currency", "USD"));
        int digits = business.get("minorUnitDigits") instanceof Number n ? n.intValue() : 2;
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
        try {
            f.setCurrency(Currency.getInstance(code));
        } catch (IllegalArgumentException e) {
            // unknown code: keep USD symbol
        }
        f.setMinimumFractionDigits(digits);
        f.setMaximumFractionDigits(digits);
        return new Money(f, digits);
    }
}
