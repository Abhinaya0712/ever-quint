import type { Task } from '../types';
const KEY = 'team-workflow-board';
const VERSION = 2;
type Stored = { schemaVersion: number; tasks: Task[] };
export const seedTasks: Task[] = [
  {
    id: 'task-1',
    title: 'Set up project repository',
    description:
      'Create the repository, add the initial application structure, and document the development workflow.',
    status: 'Done',
    priority: 'High',
    assignee: 'Alex Morgan',
    tags: ['Setup', 'Engineering'],
    createdAt: '2026-11-18T09:00:00.000Z',
    updatedAt: '2026-11-20T12:30:00.000Z',
  },
  {
    id: 'task-2',
    title: 'Design dashboard layout',
    description:
      'Create a responsive dashboard layout with clear status columns and task cards.',
    status: 'In Progress',
    priority: 'High',
    assignee: 'Sam Lee',
    tags: ['Design', 'UI'],
    createdAt: '2026-11-19T10:00:00.000Z',
    updatedAt: '2026-11-23T08:00:00.000Z',
  },
  {
    id: 'task-3',
    title: 'Add task filtering',
    description:
      'Support status, priority, and text search filters. Keep the selected filters in the URL.',
    status: 'In Progress',
    priority: 'Medium',
    assignee: 'Jordan Kim',
    tags: ['Feature'],
    createdAt: '2026-11-20T11:00:00.000Z',
    updatedAt: '2026-11-22T14:00:00.000Z',
  },
  {
    id: 'task-4',
    title: 'Write component tests',
    description:
      'Cover task creation and a meaningful filtering or status-change interaction.',
    status: 'Backlog',
    priority: 'Medium',
    assignee: 'Taylor Reed',
    tags: ['Testing', 'Quality'],
    createdAt: '2026-11-21T09:30:00.000Z',
    updatedAt: '2026-11-21T09:30:00.000Z',
  },
  {
    id: 'task-5',
    title: 'Review accessibility',
    description:
      'Check keyboard navigation, labels, focus behavior, and error messaging.',
    status: 'Backlog',
    priority: 'Low',
    assignee: '',
    tags: ['Accessibility'],
    createdAt: '2026-11-22T08:00:00.000Z',
    updatedAt: '2026-11-22T08:00:00.000Z',
  },
];
function migrate(raw: unknown): Stored {
  if (!raw || typeof raw !== 'object')
    return { schemaVersion: VERSION, tasks: seedTasks };
  const value = raw as { schemaVersion?: number; tasks?: unknown[] };
  const tasks = Array.isArray(value.tasks)
    ? (value.tasks as Task[])
    : seedTasks;
  // Version 1 stored `assignee` as null and did not guarantee tags/dates.
  const normalized = tasks.map((t) => ({
    ...t,
    assignee: typeof t.assignee === 'string' ? t.assignee : '',
    tags: Array.isArray(t.tags) ? t.tags : [],
    createdAt: t.createdAt || new Date().toISOString(),
    updatedAt: t.updatedAt || t.createdAt || new Date().toISOString(),
  }));
  return { schemaVersion: VERSION, tasks: normalized };
}
export function loadTasks(): { tasks: Task[]; migrated: boolean } {
  if (typeof window === 'undefined')
    return { tasks: seedTasks, migrated: false };
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return { tasks: seedTasks, migrated: false };
    const parsed = JSON.parse(raw) as Stored;
    const data = migrate(parsed);
    return {
      tasks: data.tasks,
      migrated: (parsed.schemaVersion ?? 1) < VERSION,
    };
  } catch {
    return { tasks: seedTasks, migrated: false };
  }
}
export function saveTasks(tasks: Task[]): void {
  window.localStorage.setItem(
    KEY,
    JSON.stringify({ schemaVersion: VERSION, tasks } satisfies Stored)
  );
}
export const STORAGE_KEY = KEY;
