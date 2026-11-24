package com.example.meetingrooms.api;

import com.example.meetingrooms.domain.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record CreateRoomRequest(@NotBlank String name, @Min(1) int capacity, int floor,
            List<@NotBlank String> amenities) {
    }

    public record RoomResponse(Long id, String name, int capacity, int floor, Set<String> amenities) {
        public static RoomResponse from(Room r) {
            return new RoomResponse(r.getId(), r.getName(), r.getCapacity(), r.getFloor(), r.getAmenities());
        }
    }

    public record CreateBookingRequest(@NotNull Long roomId, @NotBlank String title,
            @NotBlank @Email String organizerEmail, @NotNull OffsetDateTime startTime,
            @NotNull OffsetDateTime endTime) {
    }

    public record BookingResponse(Long id, Long roomId, String title, String organizerEmail, OffsetDateTime startTime,
            OffsetDateTime endTime, String status) {
        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getRoom().getId(), b.getTitle(), b.getOrganizerEmail(),
                    b.getStartTime(), b.getEndTime(), b.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }

    public record PageResponse<T>(List<T> items, long total, int limit, int offset) {
    }

    public record UtilizationItem(Long roomId, String roomName, double totalBookingHours, double utilizationPercent) {
    }

    public record ErrorResponse(String error, String message) {
    }
}
