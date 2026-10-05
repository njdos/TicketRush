//package com.rush.ticket.integration.reservation;
//
//import com.rush.ticket.integration.BaseIntegrationTest;
//import com.rush.ticket.reservation.entity.Seat;
//import com.rush.ticket.reservation.exception.SeatAlreadyReservedException;
//import com.rush.ticket.reservation.repository.SeatRepository;
//import com.rush.ticket.reservation.service.ReservationService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.List;
//import java.util.TimeZone;
//import java.util.UUID;
//import java.util.concurrent.*;
//import java.util.stream.IntStream;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//class ReservationIntegrationTestConcurrencyTest extends BaseIntegrationTest {
//
//    static {
//        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
//    }
//
//    @Autowired
//    private ReservationService reservationService;
//    @Autowired
//    private SeatRepository seatRepository;
//
//    private UUID eventId;
//    private UUID seatId;
//
//    @BeforeEach
//    void setUp() {
//        eventId = UUID.randomUUID();
//        seatId = UUID.randomUUID();
//        seatRepository.save(new Seat(seatId, eventId));
//    }
//
//    @Test
//    void onlyOneThreadWinsTheSameSeat() throws InterruptedException {
//        int threadCount = 50;
//        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//
//        CountDownLatch readyLatch = new CountDownLatch(threadCount);
//        CountDownLatch startLatch = new CountDownLatch(1);
//
//        CopyOnWriteArrayList<UUID> successfulUsers = new CopyOnWriteArrayList<>();
//        CopyOnWriteArrayList<Exception> conflicts = new CopyOnWriteArrayList<>();
//
//        List<Future<Void>> futures = IntStream.range(0, threadCount)
//                .<Callable<Void>>mapToObj(i -> () -> {
//                    readyLatch.countDown();
//                    try {
//                        startLatch.await();
//
//                        UUID userId = UUID.randomUUID();
//                        reservationService.reserveSeat(eventId, seatId, userId);
//                        successfulUsers.add(userId);
//                    } catch (SeatAlreadyReservedException e) {
//                        conflicts.add(e);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                    return null;
//                })
//                .map(executor::submit)
//                .toList();
//
//        readyLatch.await();
//        startLatch.countDown();
//
//        for (Future<Void> future : futures) {
//            try {
//                future.get(10, TimeUnit.SECONDS);
//            } catch (Exception ignored) {
//            }
//        }
//        executor.shutdown();
//
//        assertThat(successfulUsers.size()).isEqualTo(1);
//        assertThat(conflicts.size()).isEqualTo(threadCount - 1);
//    }
//}