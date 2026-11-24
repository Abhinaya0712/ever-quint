import { defaultFilters, filterTasks, readFilters, writeFilters } from './filters';
import type { Task } from '../types';

const tasks: Task[] = [
  { id: '1', title: 'Design page', description: 'Create UI', status: 'Backlog', priority: 'High', assignee: 'A', tags: [], createdAt: '2026-01-01T00:00:00.000Z', updatedAt: '2026-01-02T00:00:00.000Z' },
  { id: '2', title: 'Write tests', description: 'Cover filters', status: 'Done', priority: 'Low', assignee: 'B', tags: [], createdAt: '2026-01-02T00:00:00.000Z', updatedAt: '2026-01-03T00:00:00.000Z' },
];

test('filters tasks by status and search text', () => {
  expect(filterTasks(tasks, { ...defaultFilters, statuses: ['Backlog'], search: 'design' })).toHaveLength(1);
});

test('serializes and restores filters through URL query parameters', () => {
  const filters = { ...defaultFilters, statuses: ['Done' as const], priority: 'High' as const, search: 'release' };
  expect(readFilters(writeFilters(filters))).toEqual(filters);
});
