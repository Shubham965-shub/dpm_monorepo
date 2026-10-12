import { Star, ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'
import { SEVERITY_META, formatScore, severityMeta } from './types'

function Chip({ dotHex, label }: { dotHex: string; label: string }) {
  return (
    <span className="flex items-center gap-1 rounded-full bg-white/20 px-2 py-0.5 text-[10px] font-bold whitespace-nowrap">
      <span className="size-1.5 rounded-full" style={{ backgroundColor: dotHex }} />
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
  /** KPIs with no kpi_threshold_snapshot rows yet (see backend/README.md's "KPI thresholds"
   * section) - shown as its own count rather than silently missing from every other tier's total. */
  noThreshold: number
  /** 0-100 section QC score (unrounded); null = no data. */
  score: number | null
  /** Score change vs the previous period; null when either period has no score. */
  deltaPts: number | null
  starred: boolean
  onToggleStar: () => void
  expanded: boolean
  onToggle: () => void
  /** False for a domain carrying `scored: false` (see kpiConfig.ts's KpiDomain.scored /
   * qaKpiConfig.ts's Cross-Functional Context) - no star, no severity chips, no score/points
   * readout, and a neutral slate bar instead of the amber/rose scored styling, so it reads as a
   * different kind of row rather than a scored section that merely has no data yet. */
  scored?: boolean
}

export function SectionSummaryBar({
  category,
  title,
  critical,
  warning,
  onTrack,
  noThreshold,
  score,
  deltaPts,
  starred,
  onToggleStar,
  expanded,
  onToggle,
  scored = true,
}: SectionSummaryBarProps) {
  const isImproving = deltaPts != null && deltaPts >= 0

  return (
    <div
      className={cn(
        'flex items-center justify-between gap-4 rounded-xl px-4 py-3 text-white',
        !scored ? 'bg-slate-600' : expanded ? 'bg-rose-800' : 'bg-amber-700',
      )}
    >
      <div className="flex items-center gap-3">
        {scored ? (
          <button
            onClick={onToggleStar}
            aria-label={starred ? `Unfavorite ${title}` : `Favorite ${title}`}
            aria-pressed={starred}
            className="coach-section-star flex shrink-0 items-center justify-center"
          >
            <Star className={cn('size-4', starred ? 'fill-emerald-400 text-emerald-400' : 'text-white/60')} />
          </button>
        ) : (
          <span className="rounded-full bg-white/15 px-2 py-0.5 text-[10px] font-bold whitespace-nowrap">Context only</span>
        )}
        <div>
          <div className="text-[11px] font-semibold tracking-wide text-white/70 uppercase">{category}</div>
          <div className="text-lg font-bold">{title}</div>
        </div>
      </div>

      <div className="flex items-center gap-3">
        {scored && (
          <>
            {critical > 0 && <Chip dotHex={SEVERITY_META.action.pillDotHex} label={`${critical} critical`} />}
            {warning > 0 && <Chip dotHex={SEVERITY_META.alert.pillDotHex} label={`${warning} warning`} />}
            {onTrack > 0 && <Chip dotHex={SEVERITY_META['in-control'].pillDotHex} label={`${onTrack} on track`} />}
            {noThreshold > 0 && <Chip dotHex={severityMeta(null).pillDotHex} label={`${noThreshold} no threshold`} />}

            <div className="text-right">
              <div className="text-2xl leading-none font-bold">{formatScore(score)}</div>
              {deltaPts != null && (
                <div
                  className={cn(
                    'flex items-center justify-end gap-0.5 text-xs',
                    isImproving ? 'text-emerald-300' : 'text-white/70',
                  )}
                >
                  <span>{isImproving ? '▲' : '▼'}</span>
                  <span>{Math.abs(deltaPts).toFixed(1)} pts</span>
                </div>
              )}
            </div>
          </>
        )}

        <button
          onClick={onToggle}
          aria-label={expanded ? `Collapse ${title}` : `Expand ${title}`}
          className="coach-section-chevron flex size-7 shrink-0 items-center justify-center rounded-full bg-white/15 hover:bg-white/25"
        >
          <ChevronDown className={cn('size-4 transition-transform', expanded && 'rotate-180')} />
        </button>
      </div>
    </div>
  )
}
