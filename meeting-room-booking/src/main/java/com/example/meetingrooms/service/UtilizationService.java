package com.example.meetingrooms.service;

import com.example.meetingrooms.api.ApiDtos.UtilizationItem;
import com.example.meetingrooms.domain.*;
import com.example.meetingrooms.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Service
public class UtilizationService {
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final ZoneId zone;

    public UtilizationService(RoomRepository rooms, BookingRepository bookings,
            @Value("${meetingrooms.zone:Asia/Kolkata}") String zone) {
        this.rooms = rooms;
        this.bookings = bookings;
        this.zone = ZoneId.of(zone);
    }

    public List<UtilizationItem> report(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !from.isBefore(to))
            throw new IllegalArgumentException("from and to are required and from must be before to");
        List<UtilizationItem> result = new ArrayList<>();
        for (Room room : rooms.findAll()) {
            double booked = bookings.confirmedOverlappingRange(room.getId(), from, to).stream()
                    .mapToDouble(b -> overlapHours(b.getStartTime(), b.getEndTime(), from, to)).sum();
            double business = businessHours(from, to);
            double pct = business == 0 ? 0 : booked / business;
            result.add(new UtilizationItem(room.getId(), room.getName(), round2(booked), round4(pct)));
        }
        return result;
    }

    private double overlapHours(OffsetDateTime a, OffsetDateTime b, OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime s = a.isAfter(from) ? a : from, e = b.isBefore(to) ? b : to;
        return Math.max(0, Duration.between(s, e).toMinutes() / 60.0);
    }

    private double businessHours(OffsetDateTime from, OffsetDateTime to) {
        ZonedDateTime f = from.atZoneSameInstant(zone), t = to.atZoneSameInstant(zone);
        double total = 0;
        for (LocalDate d = f.toLocalDate(); !d.isAfter(t.toLocalDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY)
                continue;
            ZonedDateTime open = d.atTime(8, 0).atZone(zone), close = d.atTime(20, 0).atZone(zone);
            ZonedDateTime s = f.isAfter(open) ? f : open, e = t.isBefore(close) ? t : close;
            if (e.isAfter(s))
                total += Duration.between(s, e).toMinutes() / 60.0;
        }
        return total;
    }

    private double round2(double n) {
        return Math.round(n * 100.0) / 100.0;
    }

    private double round4(double n) {
        return Math.round(n * 10000.0) / 10000.0;
    }
}
