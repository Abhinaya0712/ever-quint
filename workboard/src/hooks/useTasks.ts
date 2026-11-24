import { useCallback, useEffect, useState } from 'react';
import type { Task, Status } from '../types';
import { loadTasks, saveTasks } from '../lib/storage';
import { seedTasks } from '../lib/storage';

export function useTasks() {
  const [tasks, setTasks] = useState<Task[]>(seedTasks);
  const [migrated, setMigrated] = useState(false);
  const [storageError, setStorageError] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  // Read browser-only storage after hydration; never access localStorage during SSR.
  useEffect(() => {
    const result = loadTasks();
    setTasks(result.tasks);
    setMigrated(result.migrated);
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    try {
      saveTasks(tasks);
      setStorageError(false);
    } catch {
      setStorageError(true);
    }
  }, [tasks, hydrated]);

  const add = useCallback((task: Omit<Task, 'id' | 'createdAt' | 'updatedAt'>) => {
    const now = new Date().toISOString();
    setTasks(prev => [{ ...task, id: crypto.randomUUID(), createdAt: now, updatedAt: now }, ...prev]);
  }, []);
  const update = useCallback((task: Task) => setTasks(prev => prev.map(t => t.id === task.id ? { ...task, updatedAt: new Date().toISOString() } : t)), []);
  const move = useCallback((id: string, status: Status) => setTasks(prev => prev.map(t => t.id === id ? { ...t, status, updatedAt: new Date().toISOString() } : t)), []);
  const remove = useCallback((id: string) => setTasks(prev => prev.filter(t => t.id !== id)), []);
  const clearMigration = useCallback(() => setMigrated(false), []);
  return { tasks, add, update, move, remove, migrated, clearMigration, storageError };
}
