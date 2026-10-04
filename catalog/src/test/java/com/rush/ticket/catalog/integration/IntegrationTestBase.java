package com.rush.ticket.catalog.integration;

import com.rush.ticket.catalog.CatalogApplication;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;


@SpringBootTest(
        classes = CatalogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestContainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("ALTER TABLE events DISABLE TRIGGER ALL;");
        jdbcTemplate.execute("TRUNCATE TABLE events RESTART IDENTITY CASCADE;");
        jdbcTemplate.execute("ALTER TABLE events ENABLE TRIGGER ALL;");

        jdbcTemplate.execute("ALTER TABLE outbox_events DISABLE TRIGGER ALL;");
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events RESTART IDENTITY CASCADE;");
        jdbcTemplate.execute("ALTER TABLE outbox_events ENABLE TRIGGER ALL;");
    }
}