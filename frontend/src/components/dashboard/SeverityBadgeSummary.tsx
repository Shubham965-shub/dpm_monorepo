import { SEVERITY_META, type Severity } from './types'

function SeverityCountBadge({ severity, count, label }: { severity: Severity; count: number; label: string }) {
  const meta = SEVERITY_META[severity]
  return (
    <span
      className="flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold whitespace-nowrap"
      style={{ backgroundColor: meta.paleHex, color: meta.solidHex }}
    >
      <span className="size-1.5 shrink-0 rounded-full" style={{ backgroundColor: meta.solidHex }} />
      {count} {label}
    </span>
  )
}

/** Page-level "N CRITICAL / N WARNING / N ON TRACK" summary in the persona lens views (Operator/
 * Supervisor - see the reference screenshots) - reuses SEVERITY_META's Figma-sourced palette so it
 * never invents its own colours, and the same countSeverityTiers totals SectionSummaryList's own
 * per-section chips use, so the two can never disagree. */
export function SeverityBadgeSummary({ critical, warning, onTrack }: { critical: number; warning: number; onTrack: number }) {
  return (
    <div className="flex shrink-0 items-center gap-2">
      <SeverityCountBadge severity="action" count={critical} label="CRITICAL" />
      <SeverityCountBadge severity="alert" count={warning} label="WARNING" />
      <SeverityCountBadge severity="in-control" count={onTrack} label="ON TRACK" />
    </div>
  )
}
