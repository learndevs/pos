package com.retailpos.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.retailpos.server.catalog.ProductRepository;
import com.retailpos.server.common.UuidV7;
import com.retailpos.server.sales.SalesService;
import com.retailpos.server.sync.client.SyncClient;
import com.retailpos.server.sync.contract.Contract;
import com.retailpos.server.sync.contract.Contract.CategoryV1;
import com.retailpos.server.sync.contract.Contract.Manifest;
import com.retailpos.server.sync.contract.Contract.PackageContent;
import com.retailpos.server.sync.contract.Contract.ProductV1;
import com.retailpos.server.sync.contract.PackageZip;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Till side of admin sync: package zip applied through shared rules; tampering rejected; sales queued for the archive. */
@PosIntegrationTest
@Transactional
class PackageSyncTest {

    @Autowired SyncClient sync;
    @Autowired ProductRepository products;
    @Autowired SalesService sales;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private byte[] zip(int no, PackageContent content) {
        var m = new Manifest(Contract.CONTRACT_VERSION, no, Instant.now(), "FULL", null, null,
            List.of(Contract.Entity.CATEGORY, Contract.Entity.PRODUCT), Map.of(), Map.of(), null);
        return PackageZip.write(json, m, content);
    }

    @Test
    void appliesPackageAndUpdatesOnSecondPackage() {
        UUID cat = UUID.randomUUID(), burger = UUID.randomUUID();
        var category = new CategoryV1(cat, "Synced", 1, true);
        sync.applyZip(zip(1, new PackageContent(List.of(), List.of(category),
            List.of(new ProductV1(burger, cat, null, "Burger", "MERCHANDISE", null, null, null, 1000, true, false, 0, true)))));
        sync.applyZip(zip(2, new PackageContent(List.of(), List.of(category),
            List.of(new ProductV1(burger, cat, null, "Burger", "MERCHANDISE", null, null, null, 1200, true, false, 0, true)))));
        assertThat(products.findById(burger).orElseThrow().getPriceMinor()).isEqualTo(1200);
        assertThat(sync.status().lastPackage()).isEqualTo(2);
    }

    @Test
    void rejectsTamperedPackage() {
        byte[] good = zip(3, new PackageContent(List.of(), List.of(new CategoryV1(UUID.randomUUID(), "X", 0, true)), List.of()));
        byte[] tampered = rewrite(good, "categories.json", "[{\"id\":\"00000000-0000-7000-8000-000000000999\",\"name\":\"Y\",\"sortOrder\":0,\"active\":true}]");
        assertThatThrownBy(() -> sync.applyZip(tampered)).hasMessageContaining("Checksum mismatch");
    }

    /** Same zip, one data file replaced, original manifest (and its checksums) kept. */
    private static byte[] rewrite(byte[] zip, String name, String newContent) {
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip));
             var buf = new java.io.ByteArrayOutputStream(); var out = new java.util.zip.ZipOutputStream(buf)) {
            for (java.util.zip.ZipEntry e; (e = in.getNextEntry()) != null; ) {
                byte[] data = e.getName().equals(name) ? newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8) : in.readAllBytes();
                out.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                out.write(data);
                out.closeEntry();
            }
            out.finish();
            return buf.toByteArray();
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    @Test
    void finishedSaleIsQueuedForTheArchive() {
        UUID user = UuidV7.next();
        jdbc.update("INSERT INTO app_user (id, username, display_name, pin_hash, role_id) VALUES (?, 'amy', 'Amy', 'x', '00000000-0000-7000-8000-000000000003')", user);
        jdbc.update("INSERT INTO cash_session (id, business_date, opened_by, opening_float) VALUES (?, ?, ?, 0)", UuidV7.next(), LocalDate.of(2026, 11, 19), user);
        UUID cat = UuidV7.next(), prod = UuidV7.next();
        jdbc.update("INSERT INTO category (id, name) VALUES (?, 'Mains')", cat);
        jdbc.update("INSERT INTO product (id, category_id, name, price_minor) VALUES (?, ?, 'Burger', 1000)", prod, cat);

        var s = sales.addItem(sales.start(user).id(), prod, BigDecimal.ONE);
        sales.tender(s.id(), "CASH", 1000L);
        em.flush();
        var row = jdbc.queryForMap("""
            SELECT payload->>'status' AS status, payload->>'cashierName' AS cashier, payload->'lines'->0->>'name' AS item
            FROM txn_outbox WHERE transaction_id = ?""", s.id());
        assertThat(row).containsEntry("status", "COMPLETED").containsEntry("cashier", "Amy").containsEntry("item", "Burger");
        assertThat(sync.status().outboxPending()).isEqualTo(1);
    }
}
