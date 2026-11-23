package com.example.meetingrooms.service;

import com.example.meetingrooms.api.ApiDtos.BookingResponse;
import com.example.meetingrooms.api.ApiDtos.CreateBookingRequest;
import com.example.meetingrooms.domain.Booking;
import com.example.meetingrooms.domain.Room;
import com.example.meetingrooms.repo.BookingRepository;
import com.example.meetingrooms.repo.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock RoomRepository rooms;

    private BookingService service;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, rooms, "Asia/Kolkata");
        room = mock(Room.class);
        lenient().when(room.getId()).thenReturn(1L);
        lenient().when(room.getName()).thenReturn("Board Room");
    }

    @Test
    void createsBookingWhenRoomIsAvailable() {
        CreateBookingRequest request = validRequest(10, 11);
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookings.hasOverlap(eq(1L), any(), any())).thenReturn(false);
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.create(request, "key-1");

        assertEquals("planning", response.title());
        assertEquals("confirmed", response.status());
        verify(rooms).findByIdForUpdate(1L);
        verify(bookings).hasOverlap(eq(1L), any(), any());
        verify(bookings).saveAndFlush(any(Booking.class));
    }

    @Test
    void rejectsOverlappingBookingWithConflict() {
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookings.hasOverlap(eq(1L), any(), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(validRequest(10, 11), null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBookingShorterThanFifteenMinutes() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(validRequest(10, 10, 14), null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(rooms);
    }

    @Test
    void rejectsBookingLongerThanFourHours() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(validRequest(9, 13, 1), null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(rooms);
    }

    @Test
    void rejectsStartTimeAfterEndTime() {
        CreateBookingRequest request = request(at(11, 0), at(10, 0));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(request, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsBookingOutsideBusinessHours() {
        CreateBookingRequest request = request(at(7, 0), at(8, 0));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(request, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(rooms);
    }

    @Test
    void rejectsUnknownRoom() {
        when(rooms.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(validRequest(10, 11), null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void sameIdempotencyKeyAndSameRequestReturnsExistingBooking() {
        CreateBookingRequest request = validRequest(10, 11);
        Booking existing = new Booking(room, "planning", "owner@example.com",
                request.startTime(), request.endTime(), "key-1", fingerprint(request));
        when(bookings.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        BookingResponse response = service.create(request, "key-1");

        assertEquals("confirmed", response.status());
        verify(rooms, never()).findByIdForUpdate(anyLong());
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void reusingIdempotencyKeyWithDifferentRequestReturnsConflict() {
        Booking existing = new Booking(room, "different", "owner@example.com",
                at(10, 0), at(11, 0), "key-1", "different-fingerprint");
        when(bookings.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(validRequest(10, 11), "key-1"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void cancellationAfterCutoffIsRejected() {
        OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Asia/Kolkata")).plusMinutes(30);
        Booking booking = new Booking(room, "planning", "owner@example.com",
                start, start.plusMinutes(30), null, "fingerprint");
        when(bookings.findById(10L)).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancel(10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
    }

    @Test
    void cancellationBeforeCutoffMarksBookingCancelledAndIsIdempotent() {
        OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(3);
        Booking booking = new Booking(room, "planning", "owner@example.com",
                start, start.plusMinutes(30), null, "fingerprint");
        when(bookings.findById(10L)).thenReturn(Optional.of(booking));

        BookingResponse first = service.cancel(10L);
        BookingResponse second = service.cancel(10L);

        assertEquals("cancelled", first.status());
        assertEquals("cancelled", second.status());
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
    }

    @Test
    void cancellationOfUnknownBookingReturnsNotFound() {
        when(bookings.findById(404L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.cancel(404L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private String fingerprint(CreateBookingRequest request) {
        return String.join("|", request.roomId().toString(), request.title(),
                request.organizerEmail().toLowerCase(java.util.Locale.ROOT),
                request.startTime().toString(), request.endTime().toString());
    }

    private CreateBookingRequest validRequest(int startHour, int endHour) {
        return request(at(startHour, 0), at(endHour, 0));
    }

    private CreateBookingRequest validRequest(int startHour, int endHour, int endMinute) {
        return request(at(startHour, 0), at(endHour, endMinute));
    }

    private CreateBookingRequest request(OffsetDateTime start, OffsetDateTime end) {
        return new CreateBookingRequest(1L, "planning", "owner@example.com", start, end);
    }

    private OffsetDateTime at(int hour, int minute) {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(hour, minute).atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
    }
}
