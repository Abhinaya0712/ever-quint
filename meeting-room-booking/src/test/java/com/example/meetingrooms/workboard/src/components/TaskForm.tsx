import { useState, type FormEvent } from 'react';
import type { Priority, Status, Task } from '../types';
import { Button } from './ui/Button';
import { Modal } from './ui/Modal';
type Draft=Pick<Task,'title'|'description'|'status'|'priority'|'assignee'|'tags'>;
const empty:Draft={title:'',description:'',status:'Backlog',priority:'Medium',assignee:'',tags:[]};
export function TaskForm({task,onSave,onClose}:{task?:Task;onSave:(d:Draft)=>void;onClose:()=>void}){
 const [draft,setDraft]=useState<Draft>(task?{title:task.title,description:task.description,status:task.status,priority:task.priority,assignee:task.assignee,tags:task.tags}:{...empty});const [error,setError]=useState('');
 const set=<K extends keyof Draft>(key:K,value:Draft[K])=>setDraft(p=>({...p,[key]:value}));
 function submit(e:FormEvent){e.preventDefault();if(!draft.title.trim()){setError('Enter a task title.');return;}if(draft.title.trim().length>80){setError('Title must be 80 characters or fewer.');return;}onSave({...draft,title:draft.title.trim(),description:draft.description.trim(),assignee:draft.assignee.trim(),tags:draft.tags.map(x=>x.trim()).filter(Boolean)});}
 return <Modal title={task?'Edit task':'Create a task'} onClose={onClose}><form className="task-form" onSubmit={submit} noValidate>
 <label>Title <span className="required">*</span><input autoFocus maxLength={80} value={draft.title} onChange={e=>{set('title',e.target.value);setError('');}} placeholder="e.g. Prepare project plan" aria-invalid={!!error} aria-describedby={error?'title-error':undefined}/></label>{error&&<p className="field-error" id="title-error">{error}</p>}
 <label>Description<textarea rows={4} value={draft.description} onChange={e=>set('description',e.target.value)} placeholder="Add details about this task..."/></label>
 <div className="form-grid"><label>Status<select value={draft.status} onChange={e=>set('status',e.target.value as Status)}>{['Backlog','In Progress','Done'].map(x=><option key={x}>{x}</option>)}</select></label><label>Priority<select value={draft.priority} onChange={e=>set('priority',e.target.value as Priority)}>{['Low','Medium','High'].map(x=><option key={x}>{x}</option>)}</select></label></div>
 <label>Assignee<input value={draft.assignee} onChange={e=>set('assignee',e.target.value)} placeholder="Name"/></label><label>Tags <input value={draft.tags.join(', ')} onChange={e=>set('tags',e.target.value.split(',').map(x=>x.trim()).filter(Boolean))} placeholder="Design, Frontend, QA"/><small>Separate tags with commas.</small></label>
 <footer className="form-actions"><Button type="button" onClick={onClose}>Cancel</Button><Button variant="primary" type="submit">{task?'Save changes':'Create task'}</Button></footer>
 </form></Modal>;
}
