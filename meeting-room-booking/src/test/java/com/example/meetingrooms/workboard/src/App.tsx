'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Archive,
  CircleCheck as CheckCircle2,
  CircleHelp,
  ClipboardList,
  Filter,
  Plus,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import { TaskCard } from './components/TaskCard';
import { TaskForm } from './components/TaskForm';
import { Button } from './components/ui/Button';
import { Badge } from './components/ui/Badge';
import { useTasks } from './hooks/useTasks';
import {
  defaultFilters,
  filterTasks,
  readFilters,
  writeFilters,
} from './lib/filters';
import type { Filters, Status, Task } from './types';

const statuses: Status[] = ['Backlog', 'In Progress', 'Done'];
const priorities = ['All', 'Low', 'Medium', 'High'] as const;
type FormDraft = Pick<
  Task,
  'title' | 'description' | 'status' | 'priority' | 'assignee' | 'tags'
>;

export default function App() {
  const {
    tasks,
    add,
    update,
    move,
    remove,
    migrated,
    clearMigration,
    storageError,
  } = useTasks();
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [formOpen, setFormOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | undefined>();
  const [dragOver, setDragOver] = useState<Status | null>(null);

  useEffect(() => {
    setFilters(readFilters(window.location.search));
  }, []);

  // useEffect(() => {
  //   const query = writeFilters(filters);
  //   if (window.location.search !== query)
  //     window.history.replaceState(
  //       {},
  //       '',
  //       `${window.location.pathname}${query}`
  //     );
  // }, [filters]);

  useEffect(() => {
    const timer = setTimeout(() => {
      const next = writeFilters(filters);
      const url = `${window.location.pathname}${next}${window.location.hash}`;

      if (
        window.location.pathname +
          window.location.search +
          window.location.hash !==
        url
      ) {
        window.history.replaceState(window.history.state, '', url);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [filters]);

  const visibleTasks = useMemo(
    () => filterTasks([...tasks], filters),
    [tasks, filters]
  );
  const completedCount = tasks.filter((task) => task.status === 'Done').length;
  const highPriorityCount = tasks.filter(
    (task) => task.priority === 'High'
  ).length;

  function openCreate(status: Status = 'Backlog') {
    setEditingTask(undefined);
    setFormOpen(true);
    if (status !== 'Backlog') setFilters((current) => current);
  }

  const handleEditTask = useCallback((task: Task) => {
    setEditingTask(task);
    setFormOpen(true);
  }, []);

  const handleDeleteTask = useCallback((taskId: string) => {
    remove(taskId);
  }, [remove]);

  const handleMoveTask = useCallback((taskId: string, nextStatus: Status) => {
    move(taskId, nextStatus);
  }, [move]);

  function saveTask(draft: FormDraft) {
    if (editingTask) update({ ...editingTask, ...draft });
    else add(draft);
    setFormOpen(false);
    setEditingTask(undefined);
  }

  function toggleStatus(status: Status) {
    setFilters((current) => ({
      ...current,
      statuses: current.statuses.includes(status)
        ? current.statuses.filter((item) => item !== status)
        : [...current.statuses, status],
    }));
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            <ClipboardList size={20} />
          </div>
          <div>
            <strong>Flowboard</strong>
            <span>TEAM WORKFLOW</span>
          </div>
        </div>
        <div className="topbar-right">
          <div className="avatar">TM</div>
        </div>
      </header>

      <section className="page-heading">
        <div>
          <div className="eyebrow">
            <SlidersHorizontal size={13} /> WORKSPACE OVERVIEW
          </div>
          <h1>Team workflow board</h1>
          <p>Plan, prioritize, and keep work moving forward.</p>
        </div>
        <Button variant="primary" size="lg" onClick={() => openCreate()}>
          <Plus size={16} /> New task
        </Button>
      </section>

      {(migrated || storageError) && (
        <div className="notice error-notice" role="status">
          {storageError
            ? 'Your browser could not save the latest changes.'
            : 'Your saved tasks were updated to the latest format.'}
          {migrated && (
            <button className="btn btn-ghost btn-sm" onClick={clearMigration}>
              Dismiss
            </button>
          )}
        </div>
      )}

      <section className="stats-row" aria-label="Board summary">
        <div className="stat-card">
          <div className="stat-icon">
            <ClipboardList size={18} />
          </div>
          <div>
            <span>Total tasks</span>
            <strong>{tasks.length}</strong>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <Archive size={18} />
          </div>
          <div>
            <span>In progress</span>
            <strong>
              {tasks.filter((task) => task.status === 'In Progress').length}
            </strong>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <CheckCircle2 size={18} />
          </div>
          <div>
            <span>Completed</span>
            <strong>{completedCount}</strong>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <CircleHelp size={18} />
          </div>
          <div>
            <span>High priority</span>
            <strong>{highPriorityCount}</strong>
          </div>
        </div>
      </section>

      <section className="toolbar" aria-label="Task filters">
        <div className="search-box">
          <Search size={16} />
          <input
            aria-label="Search tasks"
            value={filters.search}
            onChange={(event) =>
              setFilters((current) => ({
                ...current,
                search: event.target.value,
              }))
            }
            placeholder="Search tasks..."
          />
          <button
            aria-label="Clear search"
            onClick={() =>
              setFilters((current) => ({ ...current, search: '' }))
            }
          >
            <X size={14} />
          </button>
        </div>
        <div className="toolbar-controls">
          <div className="filter-control status-filter">
            <Filter size={14} />
            <details>
              <summary>
                Status{' '}
                {filters.statuses.length > 0 && (
                  <span className="filter-count">
                    {filters.statuses.length}
                  </span>
                )}
              </summary>
              <div className="status-options">
                {statuses.map((status) => (
                  <label key={status}>
                    <input
                      type="checkbox"
                      checked={filters.statuses.includes(status)}
                      onChange={() => toggleStatus(status)}
                    />
                    {status}
                  </label>
                ))}
              </div>
            </details>
          </div>
          <label className="filter-control">
            <span>Priority</span>
            <select
              aria-label="Priority filter"
              value={filters.priority}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  priority: event.target.value as Filters['priority'],
                }))
              }
            >
              {priorities.map((priority) => (
                <option key={priority}>{priority}</option>
              ))}
            </select>
          </label>
          <label className="filter-control sort-control">
            <span>Sort</span>
            <select
              aria-label="Sort tasks"
              value={filters.sort}
              onChange={(event) =>
                setFilters((current) => ({
                  ...current,
                  sort: event.target.value as Filters['sort'],
                }))
              }
            >
              <option value="updatedAt">Recently updated</option>
              <option value="createdAt">Recently created</option>
              <option value="priority">Priority</option>
            </select>
          </label>
        </div>
      </section>

      <section className="board" aria-label="Workflow columns">
        {statuses.map((status) => {
          const columnTasks = visibleTasks.filter(
            (task) => task.status === status
          );
          return (
            <section
              key={status}
              className={`board-column${
                dragOver === status ? ' drag-over' : ''
              }`}
              onDragOver={(event) => {
                event.preventDefault();
                setDragOver(status);
              }}
              onDragLeave={() => setDragOver(null)}
              onDrop={(event) => {
                event.preventDefault();
                const id = event.dataTransfer.getData('text/task-id');
                if (id) move(id, status);
                setDragOver(null);
              }}
            >
              <div className="column-heading">
                <div className="column-title">
                  <span
                    className={`status-dot dot-${status
                      .toLowerCase()
                      .replace(' ', '-')}`}
                  />
                  <h2>{status}</h2>
                  <span className="count">{columnTasks.length}</span>
                </div>
                <button
                  className="add-column-task"
                  aria-label={`Add task to ${status}`}
                  onClick={() => openCreate(status)}
                >
                  <Plus size={16} />
                </button>
              </div>
              <div className="column-content">
                {columnTasks.length ? (
                  columnTasks.map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      onEdit={handleEditTask}
                      onDelete={handleDeleteTask}
                      onMove={handleMoveTask}
                    />
                  ))
                ) : (
                  <div className="empty-column">
                    <div className="empty-icon">
                      <ClipboardList size={17} />
                    </div>
                    <strong>No tasks here</strong>
                    <span>Drag a task here or create a new one.</span>
                  </div>
                )}
              </div>
            </section>
          );
        })}
      </section>

      <footer className="app-footer">
        <span>
          <span className="footer-dot" />
          Local workspace
        </span>
        <span>
          {visibleTasks.length} of {tasks.length} tasks shown
        </span>
      </footer>
      <div className="help-note">
        <CircleHelp size={13} />
        Drag cards between columns to update their status.
      </div>
      {formOpen && (
        <TaskForm
          task={editingTask}
          onSave={saveTask}
          onClose={() => {
            setFormOpen(false);
            setEditingTask(undefined);
          }}
        />
      )}
    </main>
  );
}

export { Badge };
