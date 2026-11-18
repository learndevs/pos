package com.retailpos.server.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailpos.server.PosIntegrationTest;
import com.retailpos.server.common.PosException;
import com.retailpos.server.common.UuidV7;
import com.retailpos.server.identity.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Line discount rules (docs/features/line-discount.md). Burger 10.00, 10% VAT inclusive; cashier limit 10%. */
@PosIntegrationTest
@Transactional
class LineDiscountTest {

    @Autowired SalesService sales;
    @Autowired JdbcTemplate jdbc;

    SessionUser cashier;
    SessionUser manager;
    UUID burger;

    @BeforeEach
    void seed() {
        UUID userId = UuidV7.next();
        jdbc.update("INSERT INTO app_user (id, username, display_name, pin_hash, role_id) VALUES (?, 'amy', 'Amy', 'x', '00000000-0000-7000-8000-000000000003')", userId);
        jdbc.update("INSERT INTO cash_session (id, business_date, opened_by, opening_float) VALUES (?, ?, ?, 0)", UuidV7.next(), LocalDate.of(2026, 11, 19), userId);
        UUID cat = UuidV7.next();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Mains')", cat);
        UUID tax = UuidV7.next();
        jdbc.update("INSERT INTO tax_rate (id, name, rate_bp, inclusive) VALUES (?, 'VAT', 1000, true)", tax);
        burger = UuidV7.next();
        jdbc.update("INSERT INTO product (id, category_id, name, price_minor, tax_rate_id) VALUES (?, ?, 'Burger', 1000, ?)", burger, cat, tax);
        cashier = new SessionUser(userId, "amy", "Amy", "CASHIER", List.of("ORDER_CREATE"));
        manager = new SessionUser(userId, "amy", "Amy", "MANAGER", List.of("ORDER_CREATE", "DISCOUNT_UNLIMITED"));
    }

    @Test
    void cashierWithinLimitDiscountsAndRecalculatesTax() {
        SaleView s = sales.addItem(sales.start(cashier.id()).id(), burger, BigDecimal.valueOf(2));
        s = sales.discountLine(s.id(), s.lines().getFirst().id(), 10, cashier);
        assertThat(s.lines().getFirst().discountMinor()).isEqualTo(200);
        assertThat(s.totalMinor()).isEqualTo(1800);
        assertThat(s.taxMinor()).isEqualTo(164);            // 1800 x 10 / 110, half-up
    }

    @Test
    void cashierOverLimitIsRefused() {
        SaleView s = sales.addItem(sales.start(cashier.id()).id(), burger, BigDecimal.ONE);
        UUID line = s.lines().getFirst().id();
        assertThatThrownBy(() -> sales.discountLine(s.id(), line, 20, cashier))
            .isInstanceOf(PosException.class).hasMessageContaining("need a manager");
    }

    @Test
    void managerMayExceedLimit() {
        SaleView s = sales.addItem(sales.start(manager.id()).id(), burger, BigDecimal.ONE);
        s = sales.discountLine(s.id(), s.lines().getFirst().id(), 25, manager);
        assertThat(s.totalMinor()).isEqualTo(750);
    }

    @Test
    void quantityChangeKeepsPercentAndZeroRemovesIt() {
        SaleView s = sales.addItem(sales.start(cashier.id()).id(), burger, BigDecimal.ONE);
        UUID line = s.lines().getFirst().id();
        sales.discountLine(s.id(), line, 10, cashier);
        s = sales.setQuantity(s.id(), line, BigDecimal.valueOf(3));
        assertThat(s.lines().getFirst().discountMinor()).isEqualTo(300);
        assertThat(s.totalMinor()).isEqualTo(2700);
        s = sales.discountLine(s.id(), line, 0, cashier);
        assertThat(s.totalMinor()).isEqualTo(3000);
    }
}
