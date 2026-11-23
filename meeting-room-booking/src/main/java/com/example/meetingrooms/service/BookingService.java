package com.example.meetingrooms.service;

import com.example.meetingrooms.api.ApiDtos.*;
import com.example.meetingrooms.domain.*;
import com.example.meetingrooms.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class BookingService {
  private final BookingRepository bookings;
  private final RoomRepository rooms;
  private final ZoneId zone;

  public BookingService(BookingRepository bookings, RoomRepository rooms,
      @Value("${meetingrooms.zone:Asia/Kolkata}") String zone) {
    this.bookings = bookings;
    this.rooms = rooms;
    this.zone = ZoneId.of(zone);
  }

  @Transactional
  public BookingResponse create(CreateBookingRequest req, String key) {
    validateWindow(req.startTime(), req.endTime());
    String fingerprint = fingerprint(req);
    if (key != null && !key.isBlank()) {
      Optional<Booking> previous = bookings.findByIdempotencyKey(key);
      if (previous.isPresent()) {
        Booking old = previous.get();
        if (!Objects.equals(old.getRequestFingerprint(), fingerprint))
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              "Idempotency-Key was already used with a different request");
        return BookingResponse.from(old);
      }
    }
    // Lock the room row: concurrent requests for the same room serialize before
    // overlap checking.
    Room room = rooms.findByIdForUpdate(req.roomId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown room: " + req.roomId()));
    // Recheck after acquiring the room lock: another identical request may have
    // committed while we waited.
    if (key != null && !key.isBlank()) {
      Optional<Booking> afterLock = bookings.findByIdempotencyKey(key);
      if (afterLock.isPresent()) {
        Booking old = afterLock.get();
        if (!Objects.equals(old.getRequestFingerprint(), fingerprint))
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              "Idempotency-Key was already used with a different request");
        return BookingResponse.from(old);
      }
    }
    if (bookings.hasOverlap(room.getId(), req.startTime(), req.endTime()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "The room already has an overlapping confirmed booking");
    Booking saved = bookings.saveAndFlush(new Booking(room, req.title(), req.organizerEmail(), req.startTime(),
        req.endTime(), blankToNull(key), fingerprint));
    return BookingResponse.from(saved);
  }

  @Transactional
  public BookingResponse cancel(Long id) {
    Booking b = bookings.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown booking: " + id));
    if (b.getStatus() == Booking.Status.CANCELLED)
      return BookingResponse.from(b);
    if (OffsetDateTime.now(zone).isAfter(b.getStartTime().minusHours(1)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "A booking can only be cancelled up to 1 hour before startTime");
    b.cancel();
    return BookingResponse.from(b);
  }

  private void validateWindow(OffsetDateTime start, OffsetDateTime end) {
    if (!start.isBefore(end))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
    Duration d = Duration.between(start, end);
    if (d.compareTo(Duration.ofMinutes(15)) < 0 || d.compareTo(Duration.ofHours(4)) > 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Booking duration must be between 15 minutes and 4 hours");
    ZonedDateTime s = start.atZoneSameInstant(zone), e = end.atZoneSameInstant(zone);
    if (!isBusinessDay(s.toLocalDate()) || !isBusinessDay(e.toLocalDate())
        || s.toLocalTime().isBefore(LocalTime.of(8, 0)) || e.toLocalTime().isAfter(LocalTime.of(20, 0))
        || !s.toLocalDate().equals(e.toLocalDate()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Bookings are allowed Monday-Friday, 08:00-20:00 (room local time)");
  }

  private boolean isBusinessDay(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }

  private String fingerprint(CreateBookingRequest r) {
    return String.join("|", r.roomId().toString(), r.title(), r.organizerEmail().toLowerCase(Locale.ROOT),
        r.startTime().toString(), r.endTime().toString());
  }

  private String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
