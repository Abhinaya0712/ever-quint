import type { Filters, Task } from '../types';
export const defaultFilters: Filters = {statuses:[],priority:'All',search:'',sort:'updatedAt'};
export function readFilters(search:string): Filters {
 const p=new URLSearchParams(search); const valid=['Backlog','In Progress','Done'];
 return {statuses:p.getAll('status').filter((x):x is Filters['statuses'][number]=>valid.includes(x)),priority:(['Low','Medium','High'].includes(p.get('priority')||'')?p.get('priority') as Filters['priority']:'All'),search:p.get('q')||'',sort:(['createdAt','updatedAt','priority'].includes(p.get('sort')||'')?p.get('sort') as Filters['sort']:'updatedAt')};
}
export function writeFilters(f:Filters):string { const p=new URLSearchParams(); f.statuses.forEach(s=>p.append('status',s)); if(f.priority!=='All')p.set('priority',f.priority); if(f.search)p.set('q',f.search); if(f.sort!=='updatedAt')p.set('sort',f.sort); const q=p.toString(); return q?`?${q}`:''; }
const rank={Low:1,Medium:2,High:3};
export function filterTasks(tasks:Task[],f:Filters):Task[] { return tasks.filter(t=>(!f.statuses.length||f.statuses.includes(t.status))&&(f.priority==='All'||t.priority===f.priority)&&(!f.search||`${t.title} ${t.description}`.toLowerCase().includes(f.search.toLowerCase()))).sort((a,b)=>f.sort==='priority'?rank[b.priority]-rank[a.priority]:new Date(b[f.sort]).getTime()-new Date(a[f.sort]).getTime()); }
