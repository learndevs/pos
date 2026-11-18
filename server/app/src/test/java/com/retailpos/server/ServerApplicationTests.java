package com.retailpos.server;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@PosIntegrationTest
class ServerApplicationTests {

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void connectsAndMigrates() {
        assertThat(dataSource).isNotNull();
        Integer roles = jdbc.queryForObject("select count(*) from role", Integer.class);
        assertThat(roles).isEqualTo(3);
        Integer migrations = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(migrations).isGreaterThanOrEqualTo(2);
    }
}
