import type { Grain } from './kpiSchemas'
import { toIsoDate } from './kpiDateRange'
import { nextPeriodStart, periodEndDate, periodStartOf } from './periodGrid'

const GRAIN_ORDER: Grain[] = ['daily', 'weekly', 'monthly', 'quarterly', 'yearly']

/** The coarser of two grains - a KPI is judged on the slower of its own frequency and the grain on screen. */
export function coarserGrain(a: Grain, b: Grain | null | undefined): Grain {
  return b != null && GRAIN_ORDER.indexOf(b) > GRAIN_ORDER.indexOf(a) ? b : a
}

/** The date the KPI's data stopped at, when a whole period after it has already finished with nothing reported
 * (i.e. the latest data is more than one full period old at this grain) - null while the data is current.
 * `asOf` is today, or the range's end when the user picked a past range. */
export function staleSince(lastDataDate: string | null, grain: Grain, asOf: string): string | null {
  if (lastDataDate == null) return null
  const start = periodStartOf(new Date(`${lastDataDate}T00:00:00`), grain)
  const firstMissing = toIsoDate(nextPeriodStart(start, grain))
  return periodEndDate(firstMissing, grain) < asOf ? lastDataDate : null
}
