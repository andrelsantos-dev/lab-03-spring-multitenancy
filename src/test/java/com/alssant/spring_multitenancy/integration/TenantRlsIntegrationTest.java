package com.alssant.spring_multitenancy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TenantRlsIntegrationTest {
    private static final String APP_USER = "app_user";
    private static final String MIGRATION_USER = "migration_user";

    @Container
    @SuppressWarnings("resource")//managed by testcontainers
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("dbRLSTest")
            .withInitScript("db/init/00-test-init.sql");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = postgres.getJdbcUrl();

        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> MIGRATION_USER);
        registry.add("spring.flyway.password", () -> "migration_password");

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> "app_password");
    }

    @Test
    void shouldConnectToPostgres() {
        assertTrue(postgres.isRunning());
    }


    @Test
    void readCurrentUser() {
        String currentUser = jdbcTemplate.queryForObject(
                "select current_user",
                String.class
        );

        assertEquals(APP_USER,currentUser);
    }
}
