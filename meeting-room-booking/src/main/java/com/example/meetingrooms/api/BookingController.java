package com.example.meetingrooms.api;

import com.example.meetingrooms.api.ApiDtos.*;
import com.example.meetingrooms.domain.Booking;
import com.example.meetingrooms.repo.BookingRepository;
import com.example.meetingrooms.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService service;
    private final BookingRepository bookings;

    public BookingController(BookingService service, BookingRepository bookings) {
        this.service = service;
        this.bookings = bookings;
    }

    @PostMapping
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.create(req, key);
    }

    @GetMapping
    public PageResponse<BookingResponse> list(@RequestParam(required = false) Long roomId,
            @RequestParam(required = false) OffsetDateTime from, @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 100 || offset < 0)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "limit must be 1-100 and offset must be >= 0");
        if (from != null && to != null && !from.isBefore(to))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "from must be before to");
        List<BookingResponse> items = bookings.page(roomId, from, to, limit, offset).stream().map(BookingResponse::from)
                .toList();
        return new PageResponse<>(items, bookings.countPage(roomId, from, to), limit, offset);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
