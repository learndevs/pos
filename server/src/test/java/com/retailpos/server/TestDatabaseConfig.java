package com.retailpos.server;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Tests run against the pos_test database, rebuilt from the migrations on every run. */
@TestConfiguration
public class TestDatabaseConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrate(DataSource dataSource) {
        return flyway -> {
            // Safety net: `clean` drops everything, so never run it against a non-test database.
            try (Connection c = dataSource.getConnection()) {
                String db = c.getCatalog();
                if (db == null || !db.endsWith("_test")) {
                    throw new IllegalStateException("Refusing to clean database '" + db + "': tests must use a *_test database");
                }
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException(e);
            }
            flyway.clean();
            flyway.migrate();
        };
    }
}
