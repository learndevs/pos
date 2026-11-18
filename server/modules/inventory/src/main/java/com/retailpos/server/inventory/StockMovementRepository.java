package com.retailpos.server.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    /** Recipe lines for a product: what one unit consumes. Read-only projection over recipe_item. */
    interface RecipeLine {
        UUID getStockItemId();
        BigDecimal getQuantity();
    }

    @Query(value = "SELECT stock_item_id AS stockItemId, quantity FROM recipe_item WHERE product_id = :productId", nativeQuery = true)
    List<RecipeLine> recipeFor(UUID productId);
}
