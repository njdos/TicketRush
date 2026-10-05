package com.rush.ticket.catalog.integration

import com.rush.ticket.payment.PaymentApplication
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(
    classes = [PaymentApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestContainersConfiguration::class)
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun cleanUp() {
        val tables = listOf("payments", "outbox_events")

        tables.forEach { table ->
            jdbcTemplate.execute(
                """
                ALTER TABLE $table DISABLE TRIGGER ALL;
                TRUNCATE TABLE $table RESTART IDENTITY CASCADE;
                ALTER TABLE $table ENABLE TRIGGER ALL;
            """.trimIndent()
            )
        }
    }
}
