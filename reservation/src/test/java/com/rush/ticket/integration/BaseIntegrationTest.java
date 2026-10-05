package com.rush.ticket.integration;

import com.rush.ticket.reservation.ReservationApplication;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;


@SpringBootTest(
        classes = ReservationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestContainersConfiguration.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("ALTER TABLE seats DISABLE TRIGGER ALL;");
        jdbcTemplate.execute("TRUNCATE TABLE seats RESTART IDENTITY CASCADE;");
        jdbcTemplate.execute("ALTER TABLE seats ENABLE TRIGGER ALL;");

        redisTemplate.getConnectionFactory().getConnection().commands().flushDb();
    }

}