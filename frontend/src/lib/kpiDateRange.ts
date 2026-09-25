import type { Grain } from './kpiSchemas'

export interface MonthColumn {
  /** "YYYY-MM", matches the first 7 chars of a KpiCardResponse.periodStart. */
  key: string
  label: string
  /** This month's own [from, to] bounds, capped at the overall range's `to` for the current
   * (possibly partial) month - lets a whole-range KPI (chartType: 'bar', which sums over its
   * entire queried window into one row per category rather than one row per period) be queried
   * one month at a time instead of only ever showing a single combined total. */
  from: string
  to: string
}

function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// Trailing window per grain, ending today (inclusive of the current, possibly-partial period) -
// per-product decision: daily 30 days, weekly 12 weeks, monthly 6 months, quarterly 4 quarters,
// yearly 3 years.
export function getGrainDateRange(grain: Grain, today: Date = new Date()): { from: string; to: string } {
  const to = toIsoDate(today)
  switch (grain) {
    case 'daily': {
      const from = new Date(today)
      from.setDate(from.getDate() - 29)
      return { from: toIsoDate(from), to }
    }
    case 'weekly': {
      const from = new Date(today)
      from.setDate(from.getDate() - (12 * 7 - 1))
      return { from: toIsoDate(from), to }
    }
    case 'monthly': {
      const from = new Date(today.getFullYear(), today.getMonth() - 5, 1)
      return { from: toIsoDate(from), to }
    }
    case 'quarterly': {
      const quarterStartMonth = Math.floor(today.getMonth() / 3) * 3
      const from = new Date(today.getFullYear(), quarterStartMonth - 9, 1)
      return { from: toIsoDate(from), to }
    }
    case 'yearly': {
      const from = new Date(today.getFullYear() - 2, 0, 1)
      return { from: toIsoDate(from), to }
    }
  }
}

/** Trailing `monthCount` calendar months ending this month, monthly grain - e.g. the Overall
 * Heatmap's 3M/4M/5M range pill. */
export function getTrailingMonthRange(monthCount: number, today: Date = new Date()) {
  const overallTo = toIsoDate(today)
  const months: MonthColumn[] = []
  for (let i = monthCount - 1; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    const lastDayOfMonth = new Date(d.getFullYear(), d.getMonth() + 1, 0)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: d.toLocaleString('en-US', { month: 'short' }),
      from: toIsoDate(d),
      to: lastDayOfMonth < today ? toIsoDate(lastDayOfMonth) : overallTo,
    })
  }
  const from = new Date(today.getFullYear(), today.getMonth() - (monthCount - 1), 1)
  return { months, from: toIsoDate(from), to: overallTo }
}
