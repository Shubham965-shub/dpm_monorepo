import { cn } from '@/lib/utils'
import type { RagStatus } from './types'

const RAG_COLOR: Record<RagStatus, string> = {
  action: 'bg-red-500',
  alert: 'bg-amber-500',
  'in-control': 'bg-emerald-500',
  'no-data': 'bg-muted-foreground/25',
}

export function RagDot({ status, className }: { status: RagStatus; className?: string }) {
  return <span className={cn('inline-block size-2 rounded-full', RAG_COLOR[status], className)} />
}
