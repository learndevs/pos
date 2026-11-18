package com.retailpos.server.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.retailpos.server.sync.contract.PackageZip;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Admin: edit -> package (date range, products only) -> till downloads + verifies + acks -> archive upload. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminSyncFlowTest.CleanDb.class)
@Transactional
class AdminSyncFlowTest {

    @TestConfiguration
    static class CleanDb {
        @Bean
        FlywayMigrationStrategy cleanMigrate(DataSource ds) {
            return flyway -> {
                try (Connection c = ds.getConnection()) {
                    if (!c.getCatalog().endsWith("_test")) throw new IllegalStateException("Refusing to clean " + c.getCatalog());
                } catch (java.sql.SQLException e) { throw new IllegalStateException(e); }
                flyway.clean();
                flyway.migrate();
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String body(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString();
    }

    @Test
    void packageRoundTripAndArchive() throws Exception {
        mvc.perform(post("/admin/api/setup").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"displayName\":\"Admin\",\"password\":\"secret-pass\"}")).andExpect(status().isCreated());
        String auth = "Bearer " + JsonPath.read(body(mvc.perform(post("/admin/api/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"secret-pass\"}"))), "$.token");

        String catId = JsonPath.read(body(mvc.perform(post("/admin/api/categories").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Mains\",\"sortOrder\":1,\"active\":true}"))), "$.id");
        mvc.perform(post("/admin/api/products").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"categoryId\":\"" + catId + "\",\"name\":\"Burger\",\"itemClass\":\"MERCHANDISE\",\"priceMinor\":1000,\"kitchenPrint\":true,\"trackStock\":false,\"sortOrder\":0,\"active\":true}"))
            .andExpect(status().isOk());
        String terminalId = JsonPath.read(body(mvc.perform(post("/admin/api/terminals").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Till 1\"}"))), "$.id");
        String till = "Bearer " + JsonPath.read(body(mvc.perform(get("/admin/api/terminals").header("Authorization", auth))), "$[0].id").toString();
        String token = JsonPath.read(body(mvc.perform(post("/admin/api/terminals").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Till 2\"}"))), "$.token");
        String till2Id = JsonPath.read(body(mvc.perform(get("/admin/api/terminals").header("Authorization", auth))), "$[1].id");

        // products only, today: the category is added automatically; only Till 2 is targeted
        String today = LocalDate.now().toString();
        String pkg = body(mvc.perform(post("/admin/api/packages").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("{\"entities\":[\"PRODUCT\"],\"scope\":\"DATE_RANGE\",\"dateFrom\":\"" + today + "\",\"dateTo\":\"" + today
                + "\",\"terminalIds\":[\"" + till2Id + "\"],\"note\":\"price update\"}")).andExpect(status().isOk()));
        assertThat((Integer) JsonPath.read(pkg, "$.counts.PRODUCT")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(pkg, "$.counts.CATEGORY")).isEqualTo(1);
        assertThat((List<?>) JsonPath.read(pkg, "$.warnings")).hasSize(1);          // Till 2 never applied a package
        int no = JsonPath.read(pkg, "$.packageNo");

        mvc.perform(get("/sync/v1/packages").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$[0].packageNo").value(no));
        byte[] zip = mvc.perform(get("/sync/v1/packages/" + no + "/zip").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var read = PackageZip.read(json, zip);                                          // same code the till uses
        assertThat(read.content().products().getFirst().name()).isEqualTo("Burger");
        assertThat(read.content().categories().getFirst().name()).isEqualTo("Mains");

        mvc.perform(post("/sync/v1/packages/" + no + "/ack").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPLIED\"}")).andExpect(status().isOk());
        mvc.perform(get("/sync/v1/packages").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/admin/api/packages").header("Authorization", auth)).andExpect(jsonPath("$[0].targets[0].status").value("APPLIED"));

        String txn = "{\"contractVersion\":1,\"transactions\":[{\"id\":\"01a1f005-fede-7dbf-9a5f-5526279ba6b4\",\"txnNo\":7,\"receiptNo\":\"20261119-0007\","
            + "\"type\":\"SALE\",\"status\":\"COMPLETED\",\"orderType\":\"TAKEAWAY\",\"businessDate\":\"2026-11-19\",\"subtotalMinor\":1000,"
            + "\"taxMinor\":91,\"totalMinor\":1000,\"paidMinor\":1000,\"changeMinor\":0,\"lines\":[]}]}";
        mvc.perform(post("/sync/v1/transactions").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(txn))
            .andExpect(jsonPath("$.stored").value(1));
        mvc.perform(post("/sync/v1/transactions").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(txn))
            .andExpect(jsonPath("$.stored").value(0));                                 // idempotent
        mvc.perform(get("/admin/api/transactions?q=0007").header("Authorization", auth)).andExpect(jsonPath("$[0].txnNo").value(7));

        mvc.perform(get("/sync/v1/packages").header("Authorization", "Bearer wrong")).andExpect(status().isForbidden());
        assertThat(terminalId).isNotBlank();
        assertThat(till).isNotBlank();
    }
}
