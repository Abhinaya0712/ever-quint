import type { ReactNode } from 'react';
export function Badge({children,tone='neutral'}:{children:ReactNode;tone?:'neutral'|'low'|'medium'|'high'|'success'|'progress'}){return <span className={`badge badge-${tone}`}>{children}</span>}
