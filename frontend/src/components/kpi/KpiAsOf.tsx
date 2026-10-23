import type { Grain } from '@/lib/kpiSchemas'
import { emptyRuns, formatNoDataPeriod, formatNoDataRange } from '@/lib/periodFormat'
import { cn } from '@/lib/utils'

/** Which period the headline number comes from, or - when the data stopped arriving - a plain data
 * warning instead, so a stale KPI reads as a data problem to chase, not a performance one. */
export function KpiAsOf({ latestPeriodStart, staleSince, grain, className }: {
  latestPeriodStart: string | null
  staleSince: string | null
  grain: Grain
  className?: string
}) {
  if (staleSince) {
    return (
      <p className={cn('rounded bg-gray-100 px-1.5 py-0.5 font-semibold text-gray-500', className)}>
        Data not updated since {formatNoDataPeriod(staleSince, grain)}
      </p>
    )
  }
  if (!latestPeriodStart) return null
  return <p className={cn('text-muted-foreground', className)}>as of {formatNoDataPeriod(latestPeriodStart, grain)}</p>
}

/** Names the periods in the chart with nothing to measure (the line is broken there) - runs of them as ranges - so a gap
 * reads as "no data" rather than a missing or hidden period. */
export function NoDataNote({ chartData, seriesKeys, grain, className }: {
  chartData: Record<string, unknown>[]
  seriesKeys: string[]
  grain: Grain
  className?: string
}) {
  // Consecutive empty periods read as one range ("Sep 25 – 26, 2026"), not one entry each.
  const runs = emptyRuns(chartData.map((point) => ({ periodStart: String(point.periodStart), empty: seriesKeys.every((key) => point[key] == null) })))
  if (runs.length === 0) return null
  return <p className={cn('text-muted-foreground', className)}>No data: {runs.map(([first, last]) => formatNoDataRange(first, last, grain)).join('; ')}</p>
}
