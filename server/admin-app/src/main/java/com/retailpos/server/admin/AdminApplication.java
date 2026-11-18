package com.retailpos.server.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Admin panel backend. Package com.retailpos.server.admin so component/entity scanning covers the shared
 * modules on this app's classpath (catalog, sync-server, ...), and nothing POS-only (not a dependency).
 */
@SpringBootApplication(scanBasePackages = "com.retailpos.server")
@EntityScan("com.retailpos.server")                                                 // shared module entities
@EnableJpaRepositories(basePackages = "com.retailpos.server", considerNestedRepositories = true)
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
