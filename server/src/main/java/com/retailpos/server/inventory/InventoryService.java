package com.retailpos.server.inventory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final StockMovementRepository movements;
    private final Clock clock;

    public InventoryService(StockMovementRepository movements, Clock clock) {
        this.movements = movements;
        this.clock = clock;
    }

    /** Deducts recipe stock for one sold line. Must run inside the sale-completion transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deductForSale(UUID productId, BigDecimal soldQty, UUID saleItemId, UUID userId, LocalDate businessDate) {
        for (var line : movements.recipeFor(productId)) {
            movements.save(StockMovement.sale(line.getStockItemId(), line.getQuantity().multiply(soldQty),
                saleItemId, userId, businessDate, clock.instant()));
        }
    }
}
