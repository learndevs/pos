package com.retailpos.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full-context test against the pos_test database. Uses the real application.yml plus
 * application-test.yml (profile "test"), and rebuilds the schema via TestDatabaseConfig.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
public @interface PosIntegrationTest {}
