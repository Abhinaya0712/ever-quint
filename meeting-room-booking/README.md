# Meeting Room Booking Service (Java 17 + Spring Boot)

## Run

```bash
mvn spring-boot:run
```

The H2 database is file-backed at `./data/meetingrooms`, so records survive application restarts. Change `meetingrooms.zone` to the building's IANA timezone if it is not `Asia/Kolkata`.

## Endpoints

- `POST /rooms` — create room; room names are unique ignoring case.
- `GET /rooms?minCapacity=4&amenity=projector` — optional filters.
- `POST /bookings` — create a confirmed booking. Optional `Idempotency-Key` header.
- `GET /bookings?roomId=1&from=2026-11-23T08:00:00+05:30&to=2026-11-23T20:00:00+05:30&limit=20&offset=0` — paginated list.
- `POST /bookings/{id}/cancel` — cancel within the one-hour cutoff.
- `GET /reports/room-utilization?from=2026-11-23T08:00:00+05:30&to=2026-11-24T20:00:00+05:30` — utilization for every room.

## Example requests

Create room:
```json
{"name":"Orion","capacity":8,"floor":3,"amenities":["projector","whiteboard"]}
```

Create booking (send `Idempotency-Key: booking-req-001` as a header):
```json
{"roomId":1,"title":"Sprint planning","organizerEmail":"alex@example.com","startTime":"2026-11-24T10:00:00+05:30","endTime":"2026-11-24T11:00:00+05:30"}
```

## Implementation decisions / assumptions

1. Java 17, Spring Boot, Spring Data JPA and file-backed H2 are used to provide a runnable reference implementation without requiring an external database. For production, use PostgreSQL and migrations.
2. The room-local timezone is configurable with `meetingrooms.zone` (default `Asia/Kolkata`) because the supplied room model has no timezone field.
3. The business-hours rule is interpreted as a booking being on one weekday, starting no earlier than 08:00 and ending no later than 20:00 local time. A booking cannot span midnight.
4. The utilization denominator is the number of weekday business hours intersecting `[from,to]` (12 hours per full weekday). Booked time is clipped to the requested range and only confirmed bookings count. `utilizationPercent` is returned as a fraction from 0 to 1 (e.g. 0.45 means 45%).
5. Idempotency keys are globally unique. Reusing a key with an identical request returns the original booking; using it with a different request returns 409. The database unique constraint is the durable deduplication guard.
6. The room row is pessimistically locked during booking creation so two concurrent requests for the same room cannot both pass the overlap check. Cancelled bookings are excluded from overlap checks.
7. Listing time filters include bookings that intersect the requested interval, including partial overlaps. `from` and `to` are optional for listing; when both are supplied, `from` must precede `to`.

## Error status mapping

- `400`: validation failures, invalid date/time window, booking outside business hours, cancellation after cutoff.
- `404`: unknown room or booking.
- `409`: overlapping confirmed booking, duplicate room name, or idempotency key reused with a different payload.

## Suggested tests

Add integration tests for: duplicate room name with different casing; capacity and amenity filters; duration boundaries (15 minutes and 4 hours); weekday and 08:00/20:00 boundaries; overlapping and adjacent bookings; two simultaneous booking requests for the same room; same and changed payload with the same idempotency key; restart persistence; cancellation at and after the one-hour cutoff; cancelled booking no longer blocking a slot; and utilization with no bookings and partial-range overlaps.

## Automated tests

The project includes JUnit 5 tests under `src/test/java`:

- `RoomControllerTest`: request validation for blank room names and invalid capacity.
- `BookingServiceTest`: valid booking, invalid time range, minimum/maximum duration, business-hour validation, unknown room, overlap conflict, idempotency-key reuse, and cancellation cutoff/idempotent cancellation.
- `UtilizationServiceTest`: partial-overlap calculation, zero-booking utilization, and invalid report ranges.
- `BookingConcurrencyTest`: starts two concurrent requests for the same room and overlapping slot against an H2 test database; expects one success, one HTTP 409 conflict, and one stored booking.

Run the complete suite from the project root with:

```bash
mvn clean test
```

Build the application JAR with:

```bash
mvn clean package
```

The concurrency test uses a separate in-memory H2 database and does not use the persistent local database configured for normal application runs.

**Note:** These tests have been added to the project but have not been executed in this environment. Run `mvn clean test` in an environment that supports Maven builds to verify them. IntelliJ.ONLINE may compile and run supported tests, but its platform restrictions prevent starting the Spring Boot web server.
