import type { Grain } from '@/lib/kpiSchemas'
import { formatNoDataPeriod } from '@/lib/periodFormat'
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

/** Names the periods in the chart with nothing to measure (the line is broken there), so a gap
 * reads as "no data" rather than a missing or hidden period. */
export function NoDataNote({ chartData, seriesKeys, grain, className }: {
  chartData: Record<string, unknown>[]
  seriesKeys: string[]
  grain: Grain
  className?: string
}) {
  const empty = chartData
    .filter((point) => seriesKeys.every((key) => point[key] == null))
    .map((point) => formatNoDataPeriod(String(point.periodStart), grain))
  if (empty.length === 0) return null
  return <p className={cn('text-muted-foreground', className)}>No data: {empty.join(', ')}</p>
}
