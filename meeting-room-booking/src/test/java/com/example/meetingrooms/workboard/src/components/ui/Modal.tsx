import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
export function Modal({title,onClose,children}:{title:string;onClose:()=>void;children:ReactNode}){
 const ref=useRef<HTMLDivElement>(null);
 useEffect(()=>{const previous=document.activeElement as HTMLElement|null;ref.current?.focus();const onKey=(e:KeyboardEvent)=>{if(e.key==='Escape')onClose();if(e.key==='Tab'&&ref.current){const nodes=ref.current.querySelectorAll<HTMLElement>('button,input,textarea,select,[tabindex]:not([tabindex="-1"])');if(!nodes.length)return;const first=nodes[0],last=nodes[nodes.length-1];if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}}};document.addEventListener('keydown',onKey);return()=>{document.removeEventListener('keydown',onKey);previous?.focus();};},[onClose]);
 return <div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)onClose();}}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" ref={ref} tabIndex={-1}><header className="modal-header"><h2 id="modal-title">{title}</h2><button className="icon-btn" aria-label="Close dialog" onClick={onClose}><X size={18}/></button></header>{children}</section></div>;
}
