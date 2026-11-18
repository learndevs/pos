package com.retailpos.server.sales;

import com.retailpos.server.audit.AuditLog;
import com.retailpos.server.cash.CashService;
import com.retailpos.server.catalog.ProductRepository;
import com.retailpos.server.common.PosException;
import com.retailpos.server.identity.SessionUser;
import com.retailpos.server.settings.PosSettings;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sale use cases. Business rules live in PosTransaction (the aggregate); this class loads it,
 * calls it, and coordinates receipt numbering and stock on completion.
 */
@Service
@Transactional
public class SalesService {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PosTransactionRepository transactions;
    private final ProductRepository products;
    private final CashService cash;
    private final ApplicationEventPublisher events;
    private final PosSettings settings;
    private final AuditLog audit;
    private final Clock clock;

    public SalesService(PosTransactionRepository transactions, ProductRepository products, CashService cash,
                        ApplicationEventPublisher events, PosSettings settings, AuditLog audit, Clock clock) {
        this.transactions = transactions;
        this.products = products;
        this.cash = cash;
        this.events = events;
        this.settings = settings;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<SaleView> currentFor(UUID userId) {
        return openFor(userId).map(SaleView::of);
    }

    /** The user's OPEN sale, or a new one in the open cash session. */
    public SaleView start(UUID userId) {
        return SaleView.of(openFor(userId).orElseGet(() -> {
            var session = cash.requireOpen();
            return transactions.save(PosTransaction.startSale(transactions.nextTxnNo(), session.getId(), session.getBusinessDate(), userId, clock.instant()));
        }));
    }

    public SaleView addItem(UUID txnId, UUID productId, BigDecimal quantity) {
        PosTransaction t = load(txnId);
        var product = products.findByIdAndActiveTrue(productId).orElseThrow(() -> PosException.notFound("Product not found."));
        t.addSaleItem(product, quantity, clock.instant());
        return SaleView.of(t);
    }

    public SaleView setQuantity(UUID txnId, UUID itemId, BigDecimal quantity) {
        PosTransaction t = load(txnId);
        t.setQuantity(itemId, quantity);
        return SaleView.of(t);
    }

    public SaleView removeLine(UUID txnId, UUID itemId) {
        PosTransaction t = load(txnId);
        t.removeLine(itemId);
        return SaleView.of(t);
    }

    /** Line discount in whole percent (0 removes). Above the cashier limit needs DISCOUNT_UNLIMITED. Audited. */
    public SaleView discountLine(UUID txnId, UUID itemId, int percent, SessionUser user) {
        int limit = settings.cashierMaxDiscountPercent();
        if (percent > limit && !user.permissions().contains("DISCOUNT_UNLIMITED")) {
            throw PosException.forbidden("Discounts over " + limit + "% need a manager.");
        }
        PosTransaction t = load(txnId);
        SaleItem line = t.discountLine(itemId, percent);
        audit.record(user.id(), "LINE_DISCOUNT", "sale_item", line.getId(),
            "{\"percent\":" + percent + ",\"amountMinor\":" + line.getDiscountMinor() + ",\"transactionId\":\"" + txnId + "\"}");
        return SaleView.of(t);
    }

    public SaleView setOrderType(UUID txnId, String orderType) {
        PosTransaction t = load(txnId);
        t.setOrderType(orderType);
        return SaleView.of(t);
    }

    /** Adds a tender; when fully paid, completes the sale (receipt number + stock) in this same DB transaction. */
    public SaleView tender(UUID txnId, String method, Long amountMinor) {
        PosTransaction t = load(txnId);
        t.addTender(TenderItem.Method.valueOf(method), amountMinor, clock.instant());
        if (t.isFullyPaid()) complete(t);
        return SaleView.of(t);
    }

    public SaleView cancel(UUID txnId, UUID userId, String reason) {
        PosTransaction t = load(txnId);
        t.cancel(userId, reason, clock.instant());
        events.publishEvent(new TransactionFinished(t.getId(), t.getStatus()));
        return SaleView.of(t);
    }

    /** Receipt number + SaleCompleted (stock etc.) + status change, all in this one DB transaction. */
    private void complete(PosTransaction t) {
        int no = transactions.nextReceiptNo(t.getBusinessDate());
        var sold = t.saleItems().stream()
            .filter(i -> i.isMerchandise() && i.getProductId() != null)
            .map(i -> new SaleCompleted.SoldItem(i.getId(), i.getProductId(), i.getQuantity(),
                products.findById(i.getProductId()).map(p -> p.isTrackStock()).orElse(false)))
            .toList();
        events.publishEvent(new SaleCompleted(t.getId(), t.getCreatedBy(), t.getBusinessDate(), sold));
        t.complete(t.getBusinessDate().format(RECEIPT_DATE) + "-" + String.format("%04d", no), clock.instant());
        events.publishEvent(new TransactionFinished(t.getId(), t.getStatus()));
    }

    private Optional<PosTransaction> openFor(UUID userId) {
        return cash.current().flatMap(s ->
            transactions.findFirstByStatusAndCreatedByAndCashSessionIdOrderByCreatedAtDesc(PosTransaction.OPEN, userId, s.getId()));
    }

    private PosTransaction load(UUID txnId) {
        return transactions.findById(txnId).orElseThrow(() -> PosException.notFound("Sale not found."));
    }
}
