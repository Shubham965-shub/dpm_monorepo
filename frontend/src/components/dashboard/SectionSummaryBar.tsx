import { Star, ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'

function Chip({ dotColor, label }: { dotColor: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5 rounded-full bg-white/15 px-2.5 py-1 text-xs font-medium whitespace-nowrap">
      <span className={cn('size-1.5 rounded-full', dotColor)} />
      {label}
    </span>
  )
}

export interface SectionSummaryBarProps {
  category: string
  title: string
  critical: number
  warning: number
  onTrack: number
  score: number
  deltaPts: number
  starred: boolean
  expanded: boolean
  onToggle: () => void
}

export function SectionSummaryBar({
  category,
  title,
  critical,
  warning,
  onTrack,
  score,
  deltaPts,
  starred,
  expanded,
  onToggle,
}: SectionSummaryBarProps) {
  const isImproving = deltaPts >= 0

  return (
    <div
      className={cn(
        'flex items-center justify-between gap-4 rounded-xl px-4 py-3 text-white',
        expanded ? 'bg-rose-800' : 'bg-amber-700',
      )}
    >
      <div className="flex items-center gap-3">
        <Star
          className={cn('size-4 shrink-0', starred ? 'fill-emerald-400 text-emerald-400' : 'text-white/60')}
        />
        <div>
          <div className="text-[11px] font-semibold tracking-wide text-white/70 uppercase">{category}</div>
          <div className="text-lg font-bold">{title}</div>
        </div>
      </div>

      <div className="flex items-center gap-3">
        {critical > 0 && <Chip dotColor="bg-red-400" label={`${critical} critical`} />}
        {warning > 0 && <Chip dotColor="bg-amber-300" label={`${warning} warning`} />}
        {onTrack > 0 && <Chip dotColor="bg-emerald-300" label={`${onTrack} on track`} />}

        <div className="text-right">
          <div className="text-2xl leading-none font-bold">{score}</div>
          <div
            className={cn(
              'flex items-center justify-end gap-0.5 text-xs',
              isImproving ? 'text-emerald-300' : 'text-white/70',
            )}
          >
            <span>{isImproving ? '▲' : '▼'}</span>
            <span>{Math.abs(deltaPts).toFixed(1)} pts</span>
          </div>
        </div>

        <button
          onClick={onToggle}
          aria-label={expanded ? `Collapse ${title}` : `Expand ${title}`}
          className="flex size-7 shrink-0 items-center justify-center rounded-full bg-white/15 hover:bg-white/25"
        >
          <ChevronDown className={cn('size-4 transition-transform', expanded && 'rotate-180')} />
        </button>
      </div>
    </div>
  )
}
