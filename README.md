# Ever Quint — Consolidated Project README

This repository contains four assignment implementations:

1. **Meeting Room Booking Service**
2. **Water Tank Problem**
3. **Team Workflow Board + Design System**
4. **Max Fit / Max Profit Problem**

Each project is maintained in its own folder. Follow the project-specific instructions below.

---

## Repository structure

```text
ever-quint-main/
├── meeting-room-booking/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── pom.xml
│   ├── README.md
│   └── design.md
├── oneandtwo/
│   ├── index.html
│   ├── script.js
│   ├── styles.css
│   ├── max-fit-problem.js
│   └── README.md
├── workboard/
│   ├── app/
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── README.md
└── README.md
```

> The Water Tank and Max Fit / Max Profit solutions are both located in `oneandtwo/`. The repository does not contain a separate folder for each of those two solutions.

---

# 1. Meeting Room Booking Service

## Overview

A RESTful backend service for creating and managing meeting rooms and bookings. It is implemented in Java 17 with Spring Boot, Spring Data JPA, and an H2 database.

## Technology stack

- Java 17
- Spring Boot 3.5.7
- Spring Web
- Spring Data JPA
- Jakarta Bean Validation
- H2 database
- Maven
- JUnit 5 / Spring Boot Test

## Implemented capabilities

- Create rooms with name, capacity, floor, and amenities.
- Enforce case-insensitive room-name uniqueness.
- List rooms with optional minimum-capacity and amenity filters.
- Create bookings with room, title, organizer email, start time, and end time.
- Validate booking duration (15 minutes to 4 hours), time ordering, weekday, and business hours.
- Reject overlapping confirmed bookings for the same room.
- Support optional idempotent booking creation through the `Idempotency-Key` header.
- List bookings with optional room/time filters and pagination.
- Cancel bookings subject to the one-hour cancellation cutoff.
- Treat repeated cancellation as a no-op.
- Exclude cancelled bookings from overlap checks.
- Generate room-utilization reports.

## REST API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/rooms` | Create a room |
| `GET` | `/rooms` | List rooms; supports `minCapacity` and `amenity` |
| `POST` | `/bookings` | Create a booking; optionally send `Idempotency-Key` |
| `GET` | `/bookings` | List bookings; supports `roomId`, `from`, `to`, `limit`, and `offset` |
| `POST` | `/bookings/{id}/cancel` | Cancel a booking |
| `GET` | `/reports/room-utilization` | Calculate room utilization for a required `from` / `to` range |

### Example: create a room

`POST /rooms`

```json
{
  "name": "Orion",
  "capacity": 8,
  "floor": 3,
  "amenities": ["projector", "whiteboard"]
}
```

### Example: create a booking

`POST /bookings`

Header:

```text
Idempotency-Key: booking-req-001
```

Body:

```json
{
  "roomId": 1,
  "title": "Sprint planning",
  "organizerEmail": "alex@example.com",
  "startTime": "2026-11-24T10:00:00+05:30",
  "endTime": "2026-11-24T11:00:00+05:30"
}
```

## Business rules and design decisions

- Room names are normalized for case-insensitive uniqueness.
- Booking intervals use the overlap condition `existing.startTime < requested.endTime && existing.endTime > requested.startTime`; adjacent bookings are allowed.
- Only `CONFIRMED` bookings participate in overlap checks. Cancelled bookings do not block a new booking.
- A database pessimistic write lock is acquired on the room row while a booking is validated and saved. This serializes booking creation for the same room.
- An idempotency key is stored with a request fingerprint. Repeating the same key and same request returns the existing booking; reusing the key with a different request returns a conflict.
- The configured room timezone defaults to `Asia/Kolkata`.
- Business hours are interpreted as Monday–Friday, 08:00–20:00 local time. A booking cannot span midnight.
- Utilization is booked confirmed hours divided by weekday business hours in the requested interval. Booking durations are clipped to the report range. The API currently returns the ratio as a decimal fraction (for example, `0.45` represents 45%).

## Error responses

The API uses a consistent JSON error structure, for example:

```json
{
  "error": "ValidationError",
  "message": "startTime must be before endTime"
}
```

The documented status mapping is:

- `400` — invalid input, invalid time range, business-rule validation failure, or cancellation after the cutoff.
- `404` — unknown room or booking.
- `409` — overlapping confirmed booking, duplicate room name, or idempotency key reused with a different request.

## Run locally

Requirements: JDK 17 and Maven.

From the `meeting-room-booking/` directory:

```bash
mvn spring-boot:run
```

The service runs on the configured Spring Boot port (default `8080`). The default H2 database is file-backed under `./data/meetingrooms`.

## Run tests

From the `meeting-room-booking/` directory:

```bash
mvn clean test
```

Build the application:

```bash
mvn clean package
```

### Tests present in the repository

- `RoomControllerTest` — room request validation.
- `BookingServiceTest` — booking validation, time limits, business hours, unknown room, overlap, idempotency, and cancellation rules.
- `UtilizationServiceTest` — utilization calculations, partial overlaps, empty booking ranges, and invalid ranges.
- `BookingConcurrencyTest` — concurrent overlapping booking requests against an H2 test database.

The test source files are present in the repository. Run the commands above in a Maven-enabled environment to verify them; this README does not claim a test run succeeded.

More detailed design information is in [`meeting-room-booking/design.md`](meeting-room-booking/design.md).

---

# 2. Water Tank Problem

## Overview

A browser-based implementation of the trapping-rain-water problem. Users enter a comma-separated list of non-negative block heights. The page calculates the total trapped water and displays the block and water levels as SVG visualizations.

## Technology stack

- HTML5
- CSS
- Vanilla JavaScript
- SVG

No framework or package installation is required.

## Algorithm

The implementation uses prefix and suffix maximum arrays:

1. Build a left-maximum array for each position.
2. Build a right-maximum array for each position.
3. At each position, calculate trapped water as:

   `max(0, min(leftMax[i], rightMax[i]) - height[i])`

4. Sum the water values to get the total trapped water.

**Complexity**
- Time: `O(n)`
- Space: `O(n)`

## Run locally

Open `oneandtwo/index.html` directly in a browser.

Alternatively, from the `oneandtwo/` directory, start a local static server:

```bash
python -m http.server 8000
```

Then open:

```text
http://localhost:8000
```

## Input

Enter comma-separated non-negative heights, for example:

```text
0,4,0,0,0,6,0,6,4,0
```

Select **Calculate** (or press Enter) to display the total trapped water and visualization.

The current input parser retains finite non-negative numeric entries and ignores invalid or negative entries. If no valid heights remain, the page displays an invalid-input message.

## Source files

- `oneandtwo/index.html` — page structure and input controls.
- `oneandtwo/styles.css` — page styling and responsive presentation.
- `oneandtwo/script.js` — parsing, water calculation, SVG rendering, and UI event handling.

No automated test suite is currently included for this browser solution.

---

# 3. Team Workflow Board + Design System

## Overview

A client-side team task-management board built with Next.js, React, and TypeScript. It provides a Kanban-style workflow with task creation and editing, status movement, search, filters, sorting, browser persistence, and reusable UI components.

## Technology stack

- Next.js 15.5.6 (App Router)
- React 19
- TypeScript
- Tailwind CSS
- Lucide React
- Jest
- React Testing Library

## Features implemented

- Three workflow columns: Backlog, In Progress, and Done.
- Create, edit, delete, and move tasks.
- Native drag-and-drop between columns.
- Alternative status-menu task movement.
- Task title, description, priority, assignee, and tags.
- Search by task title and description.
- Multi-select status filtering.
- Priority filtering.
- Sorting by creation date, update date, or priority.
- Serialize filters into URL query parameters and restore them from the URL.
- Persist tasks in browser LocalStorage using a versioned data schema.
- Responsive layout and toast/notice feedback.
- Accessible form labels, modal interactions, validation, Escape-key handling, and focus management.
- Reusable UI components: Button, Badge, Modal, TaskCard, and TaskForm.

## Design-system components

The reusable UI primitives are located under `workboard/src/components/ui/`:

- `Button`
- `Badge`
- `Modal`

Task-focused components are under `workboard/src/components/`:

- `TaskCard`
- `TaskForm`

## Architecture

- `workboard/app/` — Next.js App Router page and root layout.
- `workboard/src/App.tsx` — board composition, filtering controls, and UI state.
- `workboard/src/components/` — task components and reusable design-system primitives.
- `workboard/src/hooks/useTasks.ts` — task state and persistence lifecycle.
- `workboard/src/lib/storage.ts` — LocalStorage access and schema migration.
- `workboard/src/lib/filters.ts` — URL filter parsing/serialization, filtering, and sorting.
- `workboard/src/types.ts` — shared task and filter types.

The board is a Client Component because it uses browser APIs such as LocalStorage, URL history, and drag-and-drop. LocalStorage is accessed after hydration.

## Run locally

Requirements: Node.js 18.18 or later (Node.js 20 LTS recommended) and npm.

From the `workboard/` directory:

```bash
npm install
npm run dev
```

Open:

```text
http://localhost:3000
```

## Production build

```bash
npm run build
npm start
```

## TypeScript check and tests

```bash
npm run lint
npm test
```

The repository includes:
- `src/App.test.tsx` — verifies the workflow board and its three columns render and that task storage is initialized.
- `src/lib/filters.test.ts` — verifies status/search filtering and URL filter serialization/restoration.

## Persistence and limitations

Tasks are stored in the current browser's LocalStorage. This implementation does not include a shared backend, authentication, or cross-user synchronization; data is browser-local.

## Rendering optimization

`TaskCard` is wrapped with `React.memo`, and stable callbacks are used to reduce avoidable child renders when parent state changes. The repository documentation notes that a before/after React Profiler recording has not been captured, so no measured performance improvement is claimed.

---

# 4. Max Fit / Max Profit Problem

## Overview

The `max-fit-problem.js` file implements a dynamic-programming solution for maximizing earnings over a fixed number of years by choosing when to construct different building types.

The source code names the function `maxProfit(n)` and defines three building options:

| Building | Construction time | Earnings value |
|---|---:|---:|
| T | 5 years | 1,500 |
| P | 4 years | 1,000 |
| C | 10 years | 2,000 |

The implementation calculates earnings based on the remaining years after a building's construction completes.

## Algorithm

The solution uses dynamic programming:

- `dp[t]` stores the maximum earnings for a construction-completion time/state `t`.
- `ways[t]` stores the building-count combinations associated with the best value for that state.
- Each building option is considered if its completion time does not exceed the supplied horizon `n`.
- The transition adds `building.earning * (n - completionTime)` to the current earnings.
- Equal-earning combinations are retained so all optimal combinations can be printed.

The program prints the maximum earnings and the optimal building-count combinations.

## Complexity

The implementation uses dynamic-programming states and stores sets of combinations for those states. Its practical time and memory use depend on the number of distinct optimal combinations retained; a simple `O(n)` complexity claim is therefore not made for the full implementation.

## Run locally

Requirements: Node.js.

From the `oneandtwo/` directory:

```bash
node max-fit-problem.js
```

The script currently runs these built-in sample horizons:

```javascript
maxProfit(7);
maxProfit(8);
maxProfit(13);
```

The function accepts the planning horizon `n` in years.

## Source file

- `oneandtwo/max-fit-problem.js` — dynamic-programming implementation and built-in sample runs.

The current implementation uses console output for its sample cases; a separate automated test framework is not included for this problem.

---

## Tooling summary

| Project | Main tools | Local run command |
|---|---|---|
| Meeting Room Booking | Java 17, Spring Boot, Maven | `mvn spring-boot:run` |
| Water Tank | HTML, CSS, JavaScript | Open `index.html` in a browser |
| Team Workflow Board | Next.js, React, TypeScript, npm | `npm run dev` |
| Max Fit / Max Profit | JavaScript, Node.js | `node max-fit-problem.js` |

## Notes

- Run each command from the corresponding project directory.
- Install the relevant runtime before running a project: JDK 17 and Maven for the backend, Node.js and npm for the Next.js application, and Node.js for the Max Fit / Max Profit script.
- The Water Tank application is a static browser application and does not require a build step.
- Automated tests are present for the Meeting Room Booking and Team Workflow Board projects. The Water Tank and Max Fit / Max Profit folders do not currently include dedicated automated test suites.
