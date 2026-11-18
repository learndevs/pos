package com.retailpos.server.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailpos.server.PosIntegrationTest;
import com.retailpos.server.common.UuidV7;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the integrity rules the database itself enforces (see docs/database-schema.md).
 * Each test runs in a transaction that is rolled back, so one failing statement per test.
 */
@PosIntegrationTest
@Transactional
class SchemaRulesTest {

    private static final UUID CASHIER_ROLE = UUID.fromString("00000000-0000-7000-8000-000000000003");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Autowired JdbcTemplate jdbc;

    UUID user;
    UUID session;
    UUID product;
    UUID category;

    @BeforeEach
    void seed() {
        user = insert("INSERT INTO app_user (id, username, display_name, pin_hash, role_id) VALUES (?, ?, 'Amy', 'x', ?)",
            UuidV7.next(), "amy", CASHIER_ROLE);
        category = insert("INSERT INTO category (id, name) VALUES (?, 'Mains')", UuidV7.next());
        product = insert("INSERT INTO product (id, category_id, name, price_minor, track_stock) VALUES (?, ?, 'Burger', 1000, true)",
            UuidV7.next(), category);
        session = insert("INSERT INTO cash_session (id, business_date, opened_by, opening_float) VALUES (?, ?, ?, 5000)",
            UuidV7.next(), TODAY, user);
    }

    // ---------- happy path ----------

    @Test
    void completedSaleShowsInSignedViews() {
        UUID txn = completedSale();
        assertThat(jdbc.queryForObject(
            "SELECT sum(amount_minor) FROM v_sale_item_signed WHERE transaction_id = ? AND item_class = 'MERCHANDISE'",
            Long.class, txn)).isEqualTo(1150L);
        assertThat(jdbc.queryForObject(
            "SELECT sum(amount_minor) FROM v_sale_item_signed WHERE transaction_id = ? AND item_class = 'NON_MERCHANDISE'",
            Long.class, txn)).isEqualTo(200L);
        assertThat(jdbc.queryForObject(
            "SELECT sum(amount_minor) FROM v_tender_signed WHERE transaction_id = ? AND method = 'CASH'",
            Long.class, txn)).isEqualTo(1350L);
    }

    @Test
    void openTransactionItemsCanBeAddedAndRemoved() {
        UUID txn = openTransaction();
        UUID item = saleLine(txn, 1, 1000);
        jdbc.update("DELETE FROM sale_item WHERE basket_item_id = ?", item);
        assertThat(jdbc.update("DELETE FROM basket_item WHERE id = ?", item)).isEqualTo(1);
    }

    @Test
    void refundLinksToOriginalSale() {
        UUID sale = completedSale();
        UUID refund = insert("""
            INSERT INTO pos_transaction (id, type, cash_session_id, business_date, order_type, created_by, reason, original_transaction_id)
            VALUES (?, 'REFUND', ?, ?, 'TAKEAWAY', ?, 'wrong order', ?)""", UuidV7.next(), session, TODAY, user, sale);
        assertThat(refund).isNotNull();
    }

    // ---------- finished transactions are frozen ----------

    @Test
    void cannotAddItemToCompletedTransaction() {
        UUID txn = completedSale();
        assertRejected("INSERT INTO basket_item (id, transaction_id, kind, line_no, amount_minor) VALUES (?, ?, 'SALE', 9, 1)",
            UuidV7.next(), txn);
    }

    @Test
    void cannotEditCompletedTransaction() {
        UUID txn = completedSale();
        assertRejected("UPDATE pos_transaction SET total_minor = 1 WHERE id = ?", txn);
    }

    @Test
    void cannotEditModifierOfCompletedTransaction() {
        completedSale();
        assertRejected("UPDATE sale_item_modifier SET price_delta_minor = 0");
    }

    @Test
    void cannotDeleteTransaction() {
        UUID txn = openTransaction();
        assertRejected("DELETE FROM pos_transaction WHERE id = ?", txn);
    }

    @Test
    void cannotEditCancelledTransaction() {
        UUID txn = openTransaction();
        jdbc.update("UPDATE pos_transaction SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ?, reason = 'left' WHERE id = ?",
            user, txn);
        assertRejected("UPDATE pos_transaction SET note = 'x' WHERE id = ?", txn);
    }

    // ---------- shape rules ----------

    @Test
    void tenderCannotAttachToSaleLine() {
        UUID item = saleLine(openTransaction(), 1, 100);
        assertRejected("INSERT INTO tender_item (basket_item_id, method) VALUES (?, 'CASH')", item);
    }

    @Test
    void nonMerchandiseNeedsType() {
        UUID item = basketItem(openTransaction(), "SALE", 1, 100);
        assertRejected("""
            INSERT INTO sale_item (basket_item_id, item_class, item_name, unit_price_minor, quantity)
            VALUES (?, 'NON_MERCHANDISE', 'x', 1, 1)""", item);
    }

    @Test
    void merchandiseNeedsProduct() {
        UUID item = basketItem(openTransaction(), "SALE", 1, 100);
        assertRejected("""
            INSERT INTO sale_item (basket_item_id, item_class, item_name, unit_price_minor, quantity)
            VALUES (?, 'MERCHANDISE', 'x', 1, 1)""", item);
    }

    @Test
    void nonMerchandiseProductCannotTrackStock() {
        assertRejected("""
            INSERT INTO product (id, category_id, name, price_minor, item_class, non_merch_type, track_stock)
            VALUES (?, ?, 'Tip', 0, 'NON_MERCHANDISE', 'TIP', true)""", UuidV7.next(), category);
    }

    @Test
    void refundNeedsOriginal() {
        assertRejected("""
            INSERT INTO pos_transaction (id, type, cash_session_id, business_date, order_type, created_by, reason)
            VALUES (?, 'REFUND', ?, ?, 'TAKEAWAY', ?, 'x')""", UuidV7.next(), session, TODAY, user);
    }

    @Test
    void lineNumbersAreUniquePerTransaction() {
        UUID txn = openTransaction();
        basketItem(txn, "SALE", 1, 100);
        assertRejected("INSERT INTO basket_item (id, transaction_id, kind, line_no, amount_minor) VALUES (?, ?, 'SALE', 1, 5)",
            UuidV7.next(), txn);
    }

    // ---------- cash session / ledgers ----------

    @Test
    void onlyOneOpenSession() {
        assertRejected("INSERT INTO cash_session (id, business_date, opened_by, opening_float) VALUES (?, ?, ?, 0)",
            UuidV7.next(), TODAY.plusDays(1), user);
    }

    @Test
    void closedSessionRejectsNewTransactions() {
        jdbc.update("UPDATE cash_session SET status = 'CLOSED', closed_at = now(), closed_by = ? WHERE id = ?", user, session);
        assertRejected("""
            INSERT INTO pos_transaction (id, cash_session_id, business_date, order_type, created_by)
            VALUES (?, ?, ?, 'TAKEAWAY', ?)""", UuidV7.next(), session, TODAY, user);
    }

    @Test
    void stockLedgerIsAppendOnly() {
        UUID stock = insert("INSERT INTO stock_item (id, name, unit) VALUES (?, 'Bun', 'pcs')", UuidV7.next());
        jdbc.update("""
            INSERT INTO stock_movement (id, stock_item_id, type, quantity_delta, user_id, business_date)
            VALUES (?, ?, 'PURCHASE', 10, ?, ?)""", UuidV7.next(), stock, user, TODAY);
        assertThat(jdbc.queryForObject("SELECT quantity FROM stock_on_hand WHERE stock_item_id = ?", Double.class, stock))
            .isEqualTo(10.0);
        assertRejected("UPDATE stock_movement SET quantity_delta = 99 WHERE stock_item_id = ?", stock);
    }

    @Test
    void cashMovementIsAppendOnly() {
        jdbc.update("""
            INSERT INTO cash_movement (id, cash_session_id, type, amount_minor, reason, user_id)
            VALUES (?, ?, 'PAID_OUT', 500, 'ice', ?)""", UuidV7.next(), session, user);
        assertRejected("DELETE FROM cash_movement");
    }

    // ---------- helpers ----------

    private UUID openTransaction() {
        return insert("""
            INSERT INTO pos_transaction (id, cash_session_id, business_date, order_type, created_by)
            VALUES (?, ?, ?, 'TAKEAWAY', ?)""", UuidV7.next(), session, TODAY, user);
    }

    /** Burger + cheese (1150), delivery fee (200), cash tender 1350 from 2000. */
    private UUID completedSale() {
        UUID txn = openTransaction();
        UUID burger = saleLine(txn, 1, 1150);
        UUID groupId = insert("INSERT INTO modifier_group (id, name, max_select) VALUES (?, 'Extras', 2)", UuidV7.next());
        UUID cheese = insert("INSERT INTO modifier (id, group_id, name, price_delta_minor) VALUES (?, ?, 'Cheese', 150)",
            UuidV7.next(), groupId);
        jdbc.update("INSERT INTO sale_item_modifier (id, sale_item_id, modifier_id, name, price_delta_minor) VALUES (?, ?, ?, 'Cheese', 150)",
            UuidV7.next(), burger, cheese);
        UUID fee = basketItem(txn, "SALE", 2, 200);
        jdbc.update("""
            INSERT INTO sale_item (basket_item_id, item_class, non_merch_type, item_name, unit_price_minor, quantity)
            VALUES (?, 'NON_MERCHANDISE', 'DELIVERY', 'Delivery fee', 200, 1)""", fee);
        UUID tender = basketItem(txn, "TENDER", 3, 1350);
        jdbc.update("INSERT INTO tender_item (basket_item_id, method, tendered_minor, change_minor) VALUES (?, 'CASH', 2000, 650)",
            tender);
        jdbc.update("UPDATE pos_transaction SET subtotal_minor = 1350, total_minor = 1350 WHERE id = ?", txn);
        jdbc.update("UPDATE pos_transaction SET status = 'COMPLETED', completed_at = now(), receipt_no = ? WHERE id = ?",
            "20261118-" + txn.toString().substring(0, 8), txn);
        return txn;
    }

    private UUID saleLine(UUID txn, int lineNo, long amount) {
        UUID item = basketItem(txn, "SALE", lineNo, amount);
        jdbc.update("""
            INSERT INTO sale_item (basket_item_id, item_class, product_id, item_name, unit_price_minor, quantity)
            VALUES (?, 'MERCHANDISE', ?, 'Burger', 1000, 1)""", item, product);
        return item;
    }

    private UUID basketItem(UUID txn, String kind, int lineNo, long amount) {
        return insert("INSERT INTO basket_item (id, transaction_id, kind, line_no, amount_minor) VALUES (?, ?, ?, ?, ?)",
            UuidV7.next(), txn, kind, lineNo, amount);
    }

    /** Runs an INSERT whose first parameter is the new row's id, and returns that id. */
    private UUID insert(String sql, Object... args) {
        jdbc.update(sql, args);
        return (UUID) args[0];
    }

    private void assertRejected(String sql, Object... args) {
        assertThatThrownBy(() -> jdbc.update(sql, args)).isInstanceOf(DataAccessException.class);
    }
}
