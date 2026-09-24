import { formatPeriodLabel } from '@/lib/periodFormat'
import type { Grain } from '@/lib/kpiSchemas'

interface TooltipPayloadEntry {
  dataKey?: string | number
  name?: string | number
  value?: number | string
  color?: string
  payload?: Record<string, unknown>
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
  return (
    <div className="rounded-md border border-border bg-card p-2 text-[11px] shadow-md">
      <div className="mb-1 font-semibold">{formatPeriodLabel(String(label), grain)}</div>
      <div className="flex flex-col gap-0.5">
        {payload.map((entry) => {
          const key = String(entry.dataKey ?? '')
          const numerator = entry.payload?.[`${key}__numerator`]
          const denominator = entry.payload?.[`${key}__denominator`]
          return (
            <div key={key} className="flex items-center gap-1.5">
              <span className="inline-block size-2 shrink-0 rounded-full" style={{ background: entry.color }} />
              <span className="font-medium">{entry.name}:</span>
              <span>{entry.value}</span>
              {typeof numerator === 'number' && typeof denominator === 'number' && (
                <span className="text-muted-foreground">
                  ({numerator.toLocaleString()} / {denominator.toLocaleString()})
                </span>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
