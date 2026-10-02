package com.rush.ticket.reservation;

import com.rush.ticket.reservation.exception.SeatAlreadyReservedException;
import com.rush.ticket.reservation.repository.SeatRepository;
import com.rush.ticket.reservation.entity.Seat;
import com.rush.ticket.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.TimeZone;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ReservationConcurrencyTest {

    static {
        // 🔥 Дублируем ультимативный фикс для этого контекста тестов
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    ReservationService reservationService;

    @Autowired
    SeatRepository seatRepository;

    UUID eventId;
    UUID seatId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        seatRepository.save(new Seat(seatId, eventId));
    }

    @Test
    void onlyOneThreadWinsTheSameSeat() throws InterruptedException {
        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<? extends Future<?>> futures = IntStream_range(threadCount)
                .mapToObj(i -> pool.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        UUID userId = UUID.randomUUID();
                        reservationService.reserveSeat(eventId, seatId, userId);
                        successCount.incrementAndGet();
                    } catch (SeatAlreadyReservedException e) {
                        conflictCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })).collect(Collectors.toList());

        readyLatch.await();
        startLatch.countDown();
        for (Future<?> f : futures) {
            try { f.get(10, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
    }

    private static IntStream IntStream_range(int n) {
        return IntStream.range(0, n);
    }
}
