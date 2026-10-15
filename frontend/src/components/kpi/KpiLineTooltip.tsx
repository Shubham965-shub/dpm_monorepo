import { formatNoDataPeriod, formatPeriodLabel } from '@/lib/periodFormat'
import type { Grain } from '@/lib/kpiSchemas'

interface TooltipPayloadEntry {
  dataKey?: string | number
  name?: string | number
  value?: number | string
  color?: string
  payload?: Record<string, unknown>
}

// Chart-data rows hold primitives only (see toChartData), so the breakdown rides as JSON.
function parseBreakdown(raw: unknown): [string, number][] | null {
  if (typeof raw !== 'string') return null
  return Object.entries(JSON.parse(raw) as Record<string, number>)
}

/**
 * Shared per-period line/bar tooltip content - shows the hovered period as a full date (see
 * formatPeriodLabel, richer than the axis tick's terse form) and, for any series carrying a
 * numerator/denominator (see KpiCardResponse - populated only for ratio KPIs), the raw counts the
 * shown value was divided from, e.g. "37.5% (45 / 120)" instead of a bare number. The companion
 * `${seriesKey}__numerator`/`__denominator` keys are stashed onto each chart-data row by
 * toChartData (KpiChartCard.tsx).
 */
export function KpiLineTooltip({
  active,
  payload,
  label,
  grain,
}: {
  active?: boolean
  payload?: readonly TooltipPayloadEntry[]
  label?: string | number
  grain: Grain
}) {
  if (!active || !payload || payload.length === 0 || label == null) return null
  const periodLabel = formatPeriodLabel(String(label), grain)
  const noDataLabel = formatNoDataPeriod(String(label), grain)
  // An empty period has no value to report - never read as a 0 (see backend PeriodCombiner).
  if (payload.every((entry) => entry.value == null)) {
    return (
      <div className="rounded-md border border-border bg-card p-2 text-[11px] shadow-md">
        <span className="text-muted-foreground">No data for {noDataLabel}</span>
      </div>
    )
  }
  return (
    <div className="rounded-md border border-border bg-card p-2 text-[11px] shadow-md">
      <div className="mb-1 font-semibold">{periodLabel}</div>
      <div className="flex flex-col gap-0.5">
        {payload.map((entry) => {
          const key = String(entry.dataKey ?? '')
          const numerator = entry.payload?.[`${key}__numerator`]
          const denominator = entry.payload?.[`${key}__denominator`]
          const breakdown = parseBreakdown(entry.payload?.[`${key}__breakdown`])
          return (
            <div key={key} className="flex items-center gap-1.5">
              <span className="inline-block size-2 shrink-0 rounded-full" style={{ background: entry.color }} />
              <span className="font-medium">{entry.name}:</span>
              {entry.value == null ? <span className="text-muted-foreground">No data</span> : <span>{entry.value}</span>}
              {breakdown ? (
                <span className="text-muted-foreground">
                  ({breakdown.map(([label, count]) => `${label} ${count.toLocaleString()}`).join(', ')})
                </span>
              ) : (
                typeof numerator === 'number' &&
                typeof denominator === 'number' && (
                  <span className="text-muted-foreground">
                    ({numerator.toLocaleString()} / {denominator.toLocaleString()})
                  </span>
                )
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
