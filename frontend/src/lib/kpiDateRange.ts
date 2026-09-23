import type { Grain } from './kpiSchemas'

export interface MonthColumn {
  /** "YYYY-MM", matches the first 7 chars of a KpiCardResponse.periodStart. */
  key: string
  label: string
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

/** Trailing 3 calendar months ending this month, monthly grain - matches the wireframe's 3-column layout. */
export function getTrailing3MonthRange(today: Date = new Date()) {
  const months: MonthColumn[] = []
  for (let i = 2; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: d.toLocaleString('en-US', { month: 'short' }),
    })
  }
  const from = new Date(today.getFullYear(), today.getMonth() - 2, 1)
  return { months, from: toIsoDate(from), to: toIsoDate(today) }
}
