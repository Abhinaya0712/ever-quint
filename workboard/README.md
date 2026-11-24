# Team Workflow Board (Next.js)

A small team task-management application built with **Next.js App Router, React, and TypeScript**. It does not use Vite.

## Requirements
- Node.js 18.18 or later (Node.js 20 LTS recommended)
- npm

## Run locally

```bash
npm install
npm run dev
```

Open http://localhost:3000.

## Production build

```bash
npm run build
npm start
```

## Tests

```bash
npm test
```

## Features
- Kanban board with Backlog, In Progress, and Done columns
- Create, edit, delete, and move tasks
- Native drag-and-drop between columns, plus a status menu alternative
- Task priority, assignee, description, and tags
- Search by title and description
- Multi-select status filter and priority filter
- Sort by created date, updated date, or priority
- Filters represented in URL query parameters and restored on refresh/back navigation
- Browser LocalStorage persistence with a versioned schema migration
- Accessible modal, labels, validation, keyboard Escape handling, and focus management
- Responsive layout and toast notifications
- Reusable Button, Badge, Modal, TaskCard, and TaskForm components

## Architecture

- `app/`: Next.js App Router entry points and document layout
- `src/App.tsx`: board composition and UI state
- `src/components/`: task UI and reusable design-system primitives
- `src/hooks/useTasks.ts`: task state and persistence lifecycle
- `src/lib/storage.ts`: LocalStorage and schema migration
- `src/lib/filters.ts`: URL serialization, filtering, and sorting
- `src/types.ts`: shared TypeScript domain types

The page is a Client Component because the board uses browser APIs (LocalStorage, URL history, drag-and-drop). The root layout remains a Server Component. LocalStorage is accessed only inside effects after hydration, so server rendering does not access browser globals.

## Data model

Each task contains `id`, `title`, `description`, `status`, `priority`, `assignee`, `tags`, `createdAt`, and `updatedAt`. Stored data is wrapped with a `schemaVersion` so older task shapes can be normalized when loaded.

## Trade-offs / next improvements
- Data is stored per browser and is not shared across team members because the assignment does not require a backend.
- Native drag-and-drop avoids an extra dependency; keyboard and menu-based status changes remain available.
- A future iteration could add IndexedDB, cross-tab synchronization, richer focus trapping, and end-to-end tests.


## Performance optimization: TaskCard rendering

### Issue investigated
The board parent re-renders when search and filter state changes. Previously, each mapped `TaskCard` received newly-created inline callback props (`onEdit`, `onDelete`, and `onMove`) on every parent render. That meant a card could re-render even when its task data had not changed.

### Change made
- Wrapped `TaskCard` in `React.memo` so it can skip rendering when its props are shallowly unchanged.
- Replaced per-card inline callbacks with shared, stable callbacks in `App.tsx`. The callbacks receive the task or task ID as an argument.
- Wrapped the task mutation functions (`add`, `update`, `move`, and `remove`) in `useCallback` inside `useTasks`, keeping the shared parent callbacks stable across renders.

### How to verify with React Developer Tools
1. Open the app in its own browser tab and open React DevTools **Profiler**.
2. Start recording, then type into Search Tasks. Stop recording and inspect the `TaskCard` entries.
3. Repeat the same interaction after this optimization. When a task's props have not changed, its `TaskCard` should be skipped by `React.memo`.
4. Also edit or move one task. The changed task card should update, while unaffected cards should be eligible to skip rendering.

### Measurement note
This change is implemented in the source. A before/after Profiler recording has **not** been captured in this environment, so no render-count or duration improvement is claimed here. Record the actual commit details and observations from the browser before reporting measured results.

### Separate profiling observation: URL synchronization
During manual debugging in StackBlitz, profiling continued when the URL synchronization effect was disabled, but reset during search when the `window.history.replaceState()` call was enabled. Preserving `window.history.state` and adding a debounce did not resolve the reset. This is recorded as an environment/integration observation, not as proof of a React rendering performance issue. URL synchronization remains in the application and should be tested separately for refresh and query-parameter persistence.
