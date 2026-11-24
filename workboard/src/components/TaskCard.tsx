import { memo, useState } from 'react';
import {
  CalendarDays,
  MoreHorizontal,
  Pencil,
  Trash2,
  UserRound,
} from 'lucide-react';
import type { Task, Status } from '../types';
import { Badge } from './ui/Badge';
type TaskCardProps = {
  task: Task;
  onEdit: (task: Task) => void;
  onDelete: (taskId: string) => void;
  onMove: (taskId: string, status: Status) => void;
};

export const TaskCard = memo(function TaskCard({
  task,
  onEdit,
  onDelete,
  onMove,
}: TaskCardProps) {
  const [menu, setMenu] = useState(false);
  const ago = relative(task.updatedAt);
  return (
    <article
      className="task-card"
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData('text/task-id', task.id);
        e.dataTransfer.effectAllowed = 'move';
      }}
    >
      <div className="task-card-top">
        <Badge
          tone={
            (task.priority ?? 'Medium').toLowerCase() as
              | 'low'
              | 'medium'
              | 'high'
          }
        >
          {task.priority} priority
        </Badge>
        <div className="menu-wrap">
          <button
            className="icon-btn small"
            aria-label={`Actions for ${task.title}`}
            aria-expanded={menu}
            onClick={() => setMenu((v) => !v)}
          >
            <MoreHorizontal size={18} />
          </button>
          {menu && (
            <div className="action-menu">
              {(['Backlog', 'In Progress', 'Done'] as Status[])
                .filter((s) => s !== task.status)
                .map((s) => (
                  <button
                    key={s}
                    onClick={() => {
                      onMove(task.id, s);
                      setMenu(false);
                    }}
                  >
                    Move to {s}
                  </button>
                ))}
              <button
                onClick={() => {
                  onEdit(task);
                  setMenu(false);
                }}
              >
                <Pencil size={14} /> Edit
              </button>
              <button
                className="danger-text"
                onClick={() => {
                  onDelete(task.id);
                  setMenu(false);
                }}
              >
                <Trash2 size={14} /> Delete
              </button>
            </div>
          )}
        </div>
      </div>
      <button className="task-title" onClick={() => onEdit(task)}>
        {task.title}
      </button>
      {task.description && (
        <p className="task-description">{task.description}</p>
      )}
      {task.tags.length > 0 && (
        <div className="tag-list">
          {task.tags.map((tag) => (
            <Badge key={tag}>{tag}</Badge>
          ))}
        </div>
      )}
      <div className="task-meta">
        <span>
          <UserRound size={14} />
          {task.assignee || 'Unassigned'}
        </span>
        <span title={new Date(task.updatedAt).toLocaleString()}>
          <CalendarDays size={14} />
          {ago}
        </span>
      </div>
    </article>
  );
});

function relative(date: string) {
  const ms = Date.now() - new Date(date).getTime();
  const h = Math.max(0, Math.floor(ms / 3600000));
  if (h < 1) return 'updated just now';
  if (h < 24) return `updated ${h}h ago`;
  const d = Math.floor(h / 24);
  return `updated ${d}d ago`;
}
