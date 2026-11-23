package com.example.meetingrooms.integration;

import com.example.meetingrooms.api.ApiDtos.BookingResponse;
import com.example.meetingrooms.api.ApiDtos.CreateBookingRequest;
import com.example.meetingrooms.domain.Room;
import com.example.meetingrooms.repo.BookingRepository;
import com.example.meetingrooms.repo.RoomRepository;
import com.example.meetingrooms.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingConcurrencyTest {
    @Autowired
    BookingService bookingService;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    BookingRepository bookingRepository;

    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void onlyOneOfTwoConcurrentOverlappingBookingsSucceeds() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room("Concurrency Room", 6, 1, Set.of("TV")));
        LocalDate date = nextWeekday();
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        OffsetDateTime start = date.atTime(10, 0).atZone(zone).toOffsetDateTime();
        OffsetDateTime end = date.atTime(11, 0).atZone(zone).toOffsetDateTime();
        CreateBookingRequest request = new CreateBookingRequest(room.getId(), "Concurrent meeting",
                "test@example.com", start, end);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        try {
            Callable<Void> task = () -> {
                ready.countDown();
                startTogether.await();
                try {
                    BookingResponse ignored = bookingService.create(request, null);
                    successes.incrementAndGet();
                } catch (ResponseStatusException ex) {
                    if (ex.getStatusCode().value() == 409)
                        conflicts.incrementAndGet();
                    else
                        throw ex;
                }
                return null;
            };
            Future<Void> first = pool.submit(task);
            Future<Void> second = pool.submit(task);
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both workers should be ready");
            startTogether.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, successes.get(), "Exactly one booking should succeed");
        assertEquals(1, conflicts.get(), "The other booking should receive HTTP 409");
        assertEquals(1, bookingRepository.count(), "Only one booking should be stored");
    }

    private LocalDate nextWeekday() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
