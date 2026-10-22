import { cn } from '@/lib/utils'
import { severityMeta, type Severity, type SeverityFilter } from './types'

function SeverityCountBadge({
  severity,
  count,
  label,
  selected,
  onSelect,
}: {
  severity: Severity | null
  count: number
  label: string
  /** True while this badge's own filter value is the currently selected one (see
   * DashboardPage's selectedSeverity). */
  selected?: boolean
  onSelect?: () => void
}) {
  const meta = severityMeta(severity)
  const content = (
    <>
      <span className="size-1.5 shrink-0 rounded-full" style={{ backgroundColor: meta.solidHex }} />
      {count} {label}
    </>
  )
  const style = selected
    ? { backgroundColor: meta.solidHex, color: 'white', boxShadow: `0 0 0 2px ${meta.solidHex}` }
    : { backgroundColor: meta.paleHex, color: meta.solidHex }
  if (!onSelect) {
    return (
      <span className="flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold whitespace-nowrap" style={style}>
        {content}
      </span>
    )
  }
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        'flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold whitespace-nowrap transition-shadow',
        'hover:brightness-95',
      )}
      style={style}
    >
      {content}
    </button>
  )
}

/** Page-level "N CRITICAL / N WARNING / N ON TRACK / N NO THRESHOLD" summary in the persona lens
 * views (Analyst/Supervisor - see the reference screenshots) - reuses SEVERITY_META's Figma-sourced
 * palette so it never invents its own colours, and the same countSeverityTiers totals
 * SectionSummaryList's own per-section chips use, so the two can never disagree.
 *
 * Every badge (including "NO THRESHOLD") doubles as a toggle (see DashboardPage's selectedSeverity):
 * clicking one auto-expands every section containing a matching KPI and narrows every section's own
 * card list down to just that bucket - same mechanism SearchKpiBar's live filter already uses to
 * force-expand matching sections, just filtering instead of merely highlighting. Clicking the
 * already-selected badge again clears the selection, back to the default view (every real-severity
 * card shown, no-threshold cards hidden - see DomainSeverityBoard). */
export function SeverityBadgeSummary({
  critical,
  warning,
  onTrack,
  noThreshold,
  selected,
  onSelect,
}: {
  critical: number
  warning: number
  onTrack: number
  /** KPIs with no kpi_threshold_snapshot rows yet - see countSeverityTiers. */
  noThreshold: number
  selected?: SeverityFilter | null
  onSelect?: (filter: SeverityFilter) => void
}) {
  return (
    <div className="flex shrink-0 flex-wrap items-center gap-2">
      <SeverityCountBadge
        severity="action"
        count={critical}
        label="CRITICAL"
        selected={selected === 'action'}
        onSelect={onSelect && (() => onSelect('action'))}
      />
      <SeverityCountBadge
        severity="alert"
        count={warning}
        label="WARNING"
        selected={selected === 'alert'}
        onSelect={onSelect && (() => onSelect('alert'))}
      />
      <SeverityCountBadge
        severity="in-control"
        count={onTrack}
        label="ON TRACK"
        selected={selected === 'in-control'}
        onSelect={onSelect && (() => onSelect('in-control'))}
      />
      {noThreshold > 0 && (
        <SeverityCountBadge
          severity={null}
          count={noThreshold}
          label="NO THRESHOLD"
          selected={selected === 'no-threshold'}
          onSelect={onSelect && (() => onSelect('no-threshold'))}
        />
      )}
    </div>
  )
}
