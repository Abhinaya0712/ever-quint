package com.example.meetingrooms.service;

import com.example.meetingrooms.api.ApiDtos.UtilizationItem;
import com.example.meetingrooms.domain.Booking;
import com.example.meetingrooms.domain.Room;
import com.example.meetingrooms.repo.BookingRepository;
import com.example.meetingrooms.repo.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilizationServiceTest {
    @Mock
    RoomRepository rooms;
    @Mock
    BookingRepository bookings;
    private UtilizationService service;
    private Room room;
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        service = new UtilizationService(rooms, bookings, "Asia/Kolkata");
        room = org.mockito.Mockito.mock(Room.class);
        lenient().when(room.getId()).thenReturn(1L);
        lenient().when(room.getName()).thenReturn("Board Room");
    }

    @Test
    void calculatesBookedHoursForPartialOverlap() {
        OffsetDateTime from = nextWeekdayAt(9, 0);
        OffsetDateTime to = nextWeekdayAt(12, 0);
        Booking booking = new Booking(room, "planning", "owner@example.com",
                from.minusHours(1), from.plusHours(1), null, "fp");
        when(rooms.findAll()).thenReturn(List.of(room));
        when(bookings.confirmedOverlappingRange(any(), eq(from), eq(to))).thenReturn(List.of(booking));

        List<UtilizationItem> result = service.report(from, to);

        assertEquals(1, result.size());
        assertEquals(1.0, result.get(0).totalBookingHours());
        assertEquals(1.0 / 3.0, result.get(0).utilizationPercent(), 0.0001);
    }

    @Test
    void returnsZeroUtilizationWhenThereAreNoBookings() {
        OffsetDateTime from = nextWeekdayAt(8, 0);
        OffsetDateTime to = nextWeekdayAt(20, 0);
        when(rooms.findAll()).thenReturn(List.of(room));
        when(bookings.confirmedOverlappingRange(any(), eq(from), eq(to))).thenReturn(List.of());

        UtilizationItem item = service.report(from, to).get(0);

        assertEquals(0.0, item.totalBookingHours());
        assertEquals(0.0, item.utilizationPercent());
    }

    @Test
    void rejectsMissingOrInvalidDateRange() {
        assertThrows(IllegalArgumentException.class, () -> service.report(null, nextWeekdayAt(12, 0)));
        OffsetDateTime time = nextWeekdayAt(10, 0);
        assertThrows(IllegalArgumentException.class, () -> service.report(time, time));
        verifyNoInteractions(rooms, bookings);
    }

    private OffsetDateTime nextWeekdayAt(int hour, int minute) {
        LocalDate date = LocalDate.now(zone).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(hour, minute).atZone(zone).toOffsetDateTime();
    }
}
