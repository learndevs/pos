package com.retailpos.server.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.retailpos.server.PosIntegrationTest;
import com.retailpos.server.common.UuidV7;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** One end-to-end sale: add items, pay cash with change, sale completes with a receipt number and stock deduction. */
@PosIntegrationTest
@Transactional
class SaleFlowTest {

    @Autowired SalesService sales;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;   // JPA writes are flushed before JDBC asserts read them

    @Test
    void cashSaleCompletesWithChangeReceiptAndStock() {
        UUID user = UuidV7.next();
        jdbc.update("INSERT INTO app_user (id, username, display_name, pin_hash, role_id) VALUES (?, 'amy', 'Amy', 'x', '00000000-0000-7000-8000-000000000003')", user);
        jdbc.update("INSERT INTO cash_session (id, business_date, opened_by, opening_float) VALUES (?, ?, ?, 0)",
            UuidV7.next(), LocalDate.of(2026, 11, 19), user);
        UUID cat = UuidV7.next();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Mains')", cat);
        UUID tax = UuidV7.next();
        jdbc.update("INSERT INTO tax_rate (id, name, rate_bp, inclusive) VALUES (?, 'VAT', 1000, true)", tax);
        UUID burger = UuidV7.next();
        jdbc.update("INSERT INTO product (id, category_id, name, price_minor, tax_rate_id, track_stock) VALUES (?, ?, 'Burger', 1100, ?, true)",
            burger, cat, tax);
        UUID bun = UuidV7.next();
        jdbc.update("INSERT INTO stock_item (id, name, unit) VALUES (?, 'Bun', 'pcs')", bun);
        jdbc.update("INSERT INTO recipe_item (product_id, stock_item_id, quantity) VALUES (?, ?, 1)", burger, bun);

        SaleView sale = sales.start(user);
        sale = sales.addItem(sale.id(), burger, BigDecimal.valueOf(2));
        assertThat(sale.totalMinor()).isEqualTo(2200);
        assertThat(sale.taxMinor()).isEqualTo(200);                  // 10% inclusive: 2200 * 1000 / 11000

        sale = sales.tender(sale.id(), "CASH", 5000L);
        assertThat(sale.status()).isEqualTo("COMPLETED");
        assertThat(sale.changeMinor()).isEqualTo(2800);
        assertThat(sale.receiptNo()).isEqualTo("20261119-0001");
        em.flush();
        assertThat(jdbc.queryForObject("SELECT quantity FROM stock_on_hand WHERE stock_item_id = ?", BigDecimal.class, bun))
            .isEqualByComparingTo("-2");
    }
}
