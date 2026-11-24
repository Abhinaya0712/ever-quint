import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { LoaderCircle } from 'lucide-react';
type Props=ButtonHTMLAttributes<HTMLButtonElement>&{variant?:'primary'|'secondary'|'danger'|'ghost';size?:'sm'|'md'|'lg';loading?:boolean;children:ReactNode};
export function Button({variant='secondary',size='md',loading=false,disabled,children,...props}:Props){return <button className={`btn btn-${variant} btn-${size}`} disabled={disabled||loading} {...props}>{loading&&<LoaderCircle size={15} className="spin"/>}{children}</button>}
