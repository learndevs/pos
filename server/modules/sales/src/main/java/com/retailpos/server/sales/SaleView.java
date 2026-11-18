package com.retailpos.server.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Everything the sale screen shows for one transaction. Money in minor units. */
public record SaleView(UUID id, long txnNo, String status, String orderType, String receiptNo, LocalDate businessDate,
                       List<Line> lines, long subtotalMinor, long taxMinor, long totalMinor,
                       long paidMinor, long dueMinor, long changeMinor, boolean cashTendered) {

    public record Line(UUID id, int lineNo, String kind, long amountMinor,
                       // SALE
                       String name, BigDecimal quantity, Long unitPriceMinor, String itemClass,
                       Long discountMinor, Integer discountPercent,
                       // TENDER
                       String tenderMethod, Long tenderedMinor, Long changeMinor) {}

    static SaleView of(PosTransaction t) {
        List<Line> lines = t.getItems().stream().map(i -> switch (i) {
            case SaleItem s -> new Line(s.getId(), s.getLineNo(), "SALE", s.getAmountMinor(), s.getItemName(),
                s.getQuantity().stripTrailingZeros().scale() <= 0 ? s.getQuantity().setScale(0) : s.getQuantity().stripTrailingZeros(),
                s.getUnitPriceMinor(), s.getItemClass().name(), s.getDiscountMinor(), s.getDiscountPercent(), null, null, null);
            case TenderItem tn -> new Line(tn.getId(), tn.getLineNo(), "TENDER", tn.getAmountMinor(), null, null, null, null, null, null,
                tn.getMethod().name(), tn.getTenderedMinor(), tn.getChangeMinor());
            default -> throw new IllegalStateException("Unknown basket item " + i.getClass());
        }).toList();
        return new SaleView(t.getId(), t.getTxnNo(), t.getStatus(), t.getOrderType(), t.getReceiptNo(), t.getBusinessDate(), lines,
            t.getSubtotalMinor(), t.getTaxMinor(), t.getTotalMinor(), t.paidMinor(), t.dueMinor(), t.changeMinor(),
            t.hasCashTender());
    }
}
