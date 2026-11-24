# Meeting Room Booking Service — Design

## 1. Overview

This project is a Spring Boot REST API for managing meeting rooms and room bookings. The implementation separates HTTP handling, business rules, persistence, and domain entities:

- `api`: REST controllers, request/response DTOs, and centralized exception handling.
- `service`: booking rules, concurrency coordination, cancellation, and utilization calculations.
- `repo`: Spring Data JPA queries and database locking.
- `domain`: JPA entities representing rooms and bookings.

The application uses Java 17, Spring Boot 3.5.7, Spring Data JPA, and H2 by default.

## 2. Data model

### Room

The `Room` entity is stored in the `rooms` table and contains:

- `id`: generated primary key.
- `name`: display name of the room.
- `nameKey`: trimmed, lowercase room name used for case-insensitive uniqueness.
- `capacity`: maximum room capacity; API validation requires at least 1.
- `floor`: floor number.
- `amenities`: element collection stored separately in `room_amenities`, associated by `room_id`.

A unique database constraint on `name_key` prevents duplicate room names after normalization.

### Booking

The `Booking` entity is stored in the `bookings` table and contains:

- `id`: generated primary key.
- `room`: mandatory many-to-one relationship to `Room` through `room_id`.
- `title` and `organizerEmail`: booking details.
- `startTime` and `endTime`: `OffsetDateTime` values defining the booking interval.
- `status`: `CONFIRMED` or `CANCELLED`.
- `idempotencyKey`: optional unique key supplied by the client.
- `requestFingerprint`: normalized request identity used to detect reuse of a key with different request data.

An index covers `room_id`, `start_time`, and `end_time` to support room/time-range lookups. A unique constraint protects non-null idempotency keys.

## 3. How overlapping bookings are prevented

The booking flow is implemented in `BookingService.create()` and runs inside `@Transactional`:

1. Validate the requested time window and business-hour rules.
2. If an idempotency key is supplied, look for a previously stored result and compare its request fingerprint.
3. Load the requested room using `RoomRepository.findByIdForUpdate()`, annotated with `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This requests a database write lock on that room row.
4. Recheck the idempotency key after acquiring the room lock.
5. Query for an overlapping booking with `BookingRepository.hasOverlap()`.
6. If a confirmed overlap exists, return HTTP 409; otherwise persist the new booking.

The overlap predicate is:

`existing.startTime < requested.endTime && existing.endTime > requested.startTime`

This uses half-open interval behaviour: a booking ending exactly when another begins does not overlap. The query filters to `CONFIRMED` bookings only, so cancelled bookings do not block a new booking for the same time.

The room-row lock serializes booking creation for the same room. Requests for different room rows can proceed independently. The lock is held until the transaction commits or rolls back. The application-level overlap check is the implemented protection; this project does not define a database exclusion constraint for time ranges.

## 4. Error handling strategy

`ApiExceptionHandler` is a `@RestControllerAdvice` that returns a consistent JSON shape:

```json
{
  "error": "ValidationError",
  "message": "startTime must be before endTime"
}
```

The handler maps common failures as follows:

- `ResponseStatusException`: preserves the status selected by the service/controller and returns its reason.
- `MethodArgumentNotValidException`: HTTP 400 with the first field validation message.
- `HttpMessageNotReadableException`: HTTP 400 for malformed JSON or date-time input.
- `IllegalArgumentException`: HTTP 400 validation error.
- `DataIntegrityViolationException`: HTTP 409 conflict for a unique-value or database-constraint violation.

The booking service uses HTTP 404 for an unknown room or booking, HTTP 409 for an overlapping booking or idempotency-key reuse with a different request, and HTTP 400 for invalid booking windows or cancellation after the cutoff.

## 5. Idempotency

`POST /bookings` accepts an optional `Idempotency-Key` header.

When a key is supplied:

1. The service creates a fingerprint from room ID, title, normalized organizer email, start time, and end time.
2. If the key already exists with the same fingerprint, the previously stored booking response is returned instead of creating another booking.
3. If the key exists with a different fingerprint, the request is rejected with HTTP 409.
4. The key and fingerprint are stored with the booking. A database unique constraint on `idempotency_key` provides a persistence-level uniqueness safeguard.

The service checks the key both before and after acquiring the room lock so a retry waiting behind another request can return the booking created by that request. If no key is supplied, this idempotent-retry behaviour is not available for that request.

## 6. Cancellation and concurrency

A booking can be cancelled only until one hour before its start time, using the configured room time zone (`meetingrooms.zone`, default `Asia/Kolkata`). A cancellation after the cutoff is rejected. Cancelling an already-cancelled booking is a no-op and returns the existing cancelled booking.

Concurrency for booking creation is coordinated with a database pessimistic write lock on the room row, acquired inside the transactional service method before checking for overlap. The transaction boundary ensures the lock remains held through the overlap check and insert, and is released by the database on commit or rollback.

This implementation serializes all booking creations for one room, including requests for non-overlapping time slots. It favours straightforward correctness over maximum parallelism for a room. It does not use Java `synchronized` or a distributed Redis lock.

## 7. Room utilization calculation

`GET /reports/room-utilization` requires `from` and `to`, with `from` earlier than `to`. For each room:

1. Fetch confirmed bookings that overlap the requested report range.
2. Clip each booking to the report range:

   `overlapStart = max(booking.startTime, from)`

   `overlapEnd = min(booking.endTime, to)`

3. Convert the positive clipped duration to hours and sum the result as `totalBookingHours`.
4. Calculate available business hours in the report range using Monday–Friday, 08:00–20:00 in the configured room time zone. Weekend hours and time outside business hours are excluded.
5. Calculate utilization as:

   `utilizationPercent = totalBookingHours / businessHours`

The API field is named `utilizationPercent`, but the current implementation returns this ratio as a decimal fraction (for example, `0.45` represents 45%), rounded to four decimal places. `totalBookingHours` is rounded to two decimal places. If the report range contains zero business hours, the ratio is returned as zero.

Assumptions: bookings are validated to remain within one local business day and business hours; only confirmed bookings contribute to utilization; the same business-hours denominator is used for every room in the requested range.

## 8. Build and test

Run the automated tests from the project root with Maven:

```bash
mvn clean test
```

Build the executable application JAR with:

```bash
mvn clean package
```

The default local database is file-backed H2, configured in `src/main/resources/application.yml`. The Dockerfile builds with Maven and runs the packaged application on the port supplied through the `PORT` environment variable (default `8080`).
