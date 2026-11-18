package com.retailpos.server.sync.client;

import com.retailpos.server.identity.AppUserRepository;
import com.retailpos.server.sales.PosTransaction;
import com.retailpos.server.sales.PosTransactionRepository;
import com.retailpos.server.sales.SaleItem;
import com.retailpos.server.sales.TenderItem;
import com.retailpos.server.sales.TransactionFinished;
import com.retailpos.server.sync.contract.Contract.TransactionLineV1;
import com.retailpos.server.sync.contract.Contract.TransactionV1;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Every finished transaction (completed or cancelled) is queued for the admin archive in the same DB transaction. */
@Component
public class TransactionArchiver {

    private final PosTransactionRepository transactions;
    private final AppUserRepository users;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    private final Clock clock;

    TransactionArchiver(PosTransactionRepository transactions, AppUserRepository users, OutboxRepository outbox,
                        ObjectMapper json, Clock clock) {
        this.transactions = transactions;
        this.users = users;
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onFinished(TransactionFinished e) {
        if (outbox.existsById(e.transactionId())) return;
        PosTransaction t = transactions.findById(e.transactionId()).orElseThrow();
        outbox.save(new OutboxEntry(t.getId(), json.writeValueAsString(toV1(t)), clock.instant()));
    }

    TransactionV1 toV1(PosTransaction t) {
        var lines = t.getItems().stream().map(i -> switch (i) {
            case SaleItem s -> new TransactionLineV1(s.getLineNo(), "SALE", s.getItemName(), s.getProductId(), s.getItemClass().name(),
                s.getQuantity(), s.getUnitPriceMinor(), s.getDiscountMinor(), s.getDiscountPercent(), s.getTaxMinor(),
                s.getAmountMinor(), null, null, null);
            case TenderItem tn -> new TransactionLineV1(tn.getLineNo(), "TENDER", null, null, null, null, null, null, null, null,
                tn.getAmountMinor(), tn.getMethod().name(), tn.getTenderedMinor(), tn.getChangeMinor());
            default -> throw new IllegalStateException("Unknown basket item " + i.getClass());
        }).toList();
        String cashier = users.findById(t.getCreatedBy()).map(u -> u.getDisplayName()).orElse(null);
        return new TransactionV1(t.getId(), t.getTxnNo(), t.getReceiptNo(), t.getType(), t.getStatus(), t.getOrderType(),
            t.getBusinessDate(), t.getCreatedAt(), t.getCompletedAt(), t.getCancelledAt(), t.getReason(), t.getCreatedBy(),
            cashier, t.getSubtotalMinor(), t.getTaxMinor(), t.getTotalMinor(), t.paidMinor(), t.changeMinor(), lines);
    }
}
